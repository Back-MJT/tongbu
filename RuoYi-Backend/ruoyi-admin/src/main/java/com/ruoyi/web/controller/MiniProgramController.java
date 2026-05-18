package com.ruoyi.web.controller;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.tenant.TenantContextHolder;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.web.service.SysLoginService;
import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.system.service.ISysUserService;
import com.ruoyi.iot.domain.entity.DeviceGroup;
import com.ruoyi.iot.domain.entity.Equipment;
import com.ruoyi.iot.domain.model.EquipmentResolveResult;
import com.ruoyi.iot.service.IDeviceGroupService;
import com.ruoyi.iot.service.IEquipmentService;
import com.ruoyi.iot.service.IMiniEquipmentService;
import com.ruoyi.intervention.service.impl.MiniProgramServiceImpl;
import com.ruoyi.intervention.entity.IntervPrescription;
import com.ruoyi.intervention.mapper.IntervPrescriptionMapper;
import com.ruoyi.intervention.service.aline.AlineAnalysisGateway;
import com.ruoyi.intervention.service.aline.AlineAnalysisRequest;
import com.ruoyi.intervention.service.aline.AlineAnalysisResult;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.Resource;

/**
 * 微信小程序 API (XIN-147)
 * 提供小程序专用的轻量接口，作为 B Line 场景平台 API。
 *
 * <p>架构:
 * <ul>
 *   <li>小程序前端 → RuoYi-Backend</li>
 *   <li>RuoYi-Backend 负责认证、多租户、DB持久化与 B Line 训练闭环</li>
 *   <li>A Line API 未提供前，仅通过 AlineAnalysisGateway stub 预留接入点</li>
 * </ul>
 *
 * <p>多租户策略: 通过 Authorization header 的 Bearer token 识别用户，
 * JWT token 中包含 userId + tenantId，解析后在 B Line 内部传递。
 */
@Anonymous
@RestController
@RequestMapping("/api")
public class MiniProgramController extends BaseController
{
    private static final Logger log = LoggerFactory.getLogger(MiniProgramController.class);

    // Demo token — used only for demo/preview mode when no real auth is available
    private static final String DEMO_TOKEN = "mp_demo_token_2026";
    private static final Long   DEMO_TENANT_ID = 1L;
    private static final String DEMO_USER_ID   = "mp_demo_001";
    private static final int EQUIPMENT_OCCUPANCY_TTL_SECONDS = 90;
    private static final String EQUIPMENT_OCCUPANCY_PREFIX = "xindong:equipment:occupancy:";
    private static final int MP_AVATAR_MAX_LENGTH = 512;

    // ─── Spring Dependencies ──────────────────────────────────────────────────────

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private SysLoginService loginService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private MiniProgramServiceImpl miniProgramService;

    @Autowired
    private IMiniEquipmentService miniEquipmentService;

    @Autowired
    private IDeviceGroupService deviceGroupService;

    @Autowired
    private IEquipmentService equipmentService;

    @Autowired
    private IntervPrescriptionMapper prescriptionMapper;

    @Autowired
    private AlineAnalysisGateway alineAnalysisGateway;

    @Value("${wechat.miniapp.appid:}")
    private String wechatMiniAppId;

    @Value("${wechat.miniapp.secret:}")
    private String wechatMiniAppSecret;

    // Inject token secret directly to avoid private-method access
    @Value("${token.secret:YangGuijie5201314YangGuijie5201314YangGuijie5201314YangGuijie5201314}")
    private String jwtSecret;

    // ─── Demo / Preview Fallback Data ───────────────────────────────────────────
    // Kept for demo mode only. Real user data comes from sys_user table.

    private static final Map<String, Map<String, Object>> DEMO_USERS = new HashMap<>();

    static {
        Map<String, Object> demoUser = new LinkedHashMap<>();
        demoUser.put("userId", DEMO_USER_ID);
        demoUser.put("tenantId", DEMO_TENANT_ID);
        demoUser.put("nickname", "张先生");
        demoUser.put("avatar", "");
        demoUser.put("level", 3);
        demoUser.put("streakDays", 7);
        demoUser.put("complianceRate", 72);
        demoUser.put("totalSessions", 23);
        demoUser.put("stage", "growth");
        demoUser.put("stageLabel", "成长期");
        demoUser.put("age", 35);
        demoUser.put("gender", "male");
        demoUser.put("deviceType", "strength_station");
        DEMO_USERS.put(DEMO_TOKEN, demoUser);
        DEMO_USERS.put(DEMO_USER_ID, demoUser);
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // Authentication / 认证
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * 微信小程序登录
     * POST /api/mini/auth/wechat-login
     * Body: { code: string }  // 微信wx.login()返回的code
     *
     * <p>实现策略:
     * <ul>
     *   <li>生产: 调用微信 jscode2session，按 openid 查找/创建本地 sys_user，返回 RuoYi token</li>
     *   <li>本地开发: 未配置 appid/secret 时，用 code 派生 dev openid，便于开发者工具联调</li>
     * </ul>
     */
    @PostMapping({"/mini/auth/wechat-login", "/wx/login"})
    public AjaxResult wxLogin(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        if (StringUtils.isEmpty(code)) {
            return AjaxResult.error("code不能为空");
        }

        WechatSession session = resolveWechatSession(code);
        if (session == null || StringUtils.isEmpty(session.openid)) {
            return AjaxResult.error("微信登录失败，未获取到openid");
        }

        String avatar = session.devMode ? null : sanitizeMpAvatar(body.get("avatar"));
        SysUser sysUser = findOrCreateMpUser(session.openid, null, body.get("nickname"), avatar, "wechat");
        if (sysUser == null) {
            // DB unavailable — fall back to demo token (BD demo mode)
            log.warn("wxLogin: DB unavailable, returning demo token");
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("token", DEMO_TOKEN);
            data.put("userId", DEMO_USER_ID);
            data.put("tenantId", DEMO_TENANT_ID);
            data.put("isDemo", true);
            return AjaxResult.success("登录成功", data);
        }

        // Generate real JWT token via RuoYi TokenService
        String token = generateMpToken(sysUser);
        log.info("wxLogin: openid={}, userId={}, tenantId={} -> token generated",
                 session.openid, sysUser.getUserId(), sysUser.getTenantId());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", token);
        data.put("userId", String.valueOf(sysUser.getUserId()));
        data.put("tenantId", sysUser.getTenantId());
        data.put("isDemo", false);
        String nickname = sysUser.getNickName();
        boolean hasCustomNickname = hasCustomNickname(nickname);
        data.put("nickname", hasCustomNickname ? nickname : "");
        data.put("needsProfileSetup", !hasCustomNickname);
        data.put("avatar", sysUser.getAvatar() != null ? sysUser.getAvatar() : "");
        data.put("openId", session.openid);
        data.put("wechatOpenId", session.openid);
        data.put("wechatOpenIdMasked", maskOpenid(session.openid));
        data.put("openid", maskOpenid(session.openid));
        if (!StringUtils.isEmpty(session.unionid)) {
            data.put("unionId", session.unionid);
            data.put("wechatUnionId", session.unionid);
        }
        data.put("wechatSessionSource", session.devMode ? "dev" : "wechat");
        return AjaxResult.success("登录成功", data);
    }

    /**
     * 手机号登录
     * POST /api/auth/phone-login
     * Body: { phone: string, verifyCode: string }
     *
     * <p>MVP阶段: 验证码任意4位以上即可（方便演示）
     * 生产环境: 校验短信网关验证码正确性
     */
    @PostMapping("/auth/phone-login")
    public AjaxResult phoneLogin(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        String verifyCode = body.get("verifyCode");
        if (StringUtils.isEmpty(phone) || phone.length() != 11) {
            return AjaxResult.error("请输入正确的手机号");
        }
        if (StringUtils.isEmpty(verifyCode) || verifyCode.length() < 4) {
            return AjaxResult.error("请输入验证码");
        }
        // TODO (production): validate verifyCode against SMS gateway

        // Find or create user by phone number
        SysUser sysUser = findOrCreateMpUser(null, phone, null, null, "phone");
        if (sysUser == null) {
            // DB unavailable — demo mode
            log.warn("phoneLogin: DB unavailable, returning demo token");
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("token", DEMO_TOKEN);
            data.put("userId", DEMO_USER_ID);
            data.put("tenantId", DEMO_TENANT_ID);
            data.put("isDemo", true);
            return AjaxResult.success("登录成功", data);
        }

        String token = generateMpToken(sysUser);
        log.info("phoneLogin: phone={}, userId={}, tenantId={}", phone, sysUser.getUserId(), sysUser.getTenantId());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", token);
        data.put("userId", String.valueOf(sysUser.getUserId()));
        data.put("tenantId", sysUser.getTenantId());
        data.put("isDemo", false);
        return AjaxResult.success("登录成功", data);
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // User / 用户
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * 获取当前用户信息
     * GET /api/user/current
     */
    @GetMapping("/user/current")
    public AjaxResult getCurrentUser(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        AuthContext ctx = resolveAuth(auth);

        // 如果没有 token，返回 401，让小程序决定是使用演示模式还是重新登录
        if (ctx == null) {
            return AjaxResult.error(401, "未登录");
        }

        if (ctx.isDemo) {
            return AjaxResult.success(DEMO_USERS.get(DEMO_TOKEN));
        }

        // Load real user from sys_user
        SysUser sysUser = userService.selectUserById(ctx.userId);
        if (sysUser == null) {
            return AjaxResult.error(401, "用户不存在");
        }
        return AjaxResult.success(buildMpUserInfo(sysUser));
    }

    /**
     * 获取当前用户真实训练统计
     * GET /api/user/training-stats
     */
    @GetMapping("/user/training-stats")
    public AjaxResult getCurrentUserTrainingStats(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        AuthContext ctx = resolveAuth(auth);
        if (ctx == null) return AjaxResult.error(401, "未登录");

        if (ctx.isDemo) {
            Map<String, Object> demo = new LinkedHashMap<>();
            demo.put("userId", DEMO_USER_ID);
            demo.put("totalSessions", 23);
            demo.put("totalSets", 156);
            demo.put("totalReps", 1400);
            demo.put("totalDurationMin", 920);
            demo.put("peakVolumeKg", 80);
            demo.put("algorithmVersion", "XIN-RULE-demo");
            return AjaxResult.success(demo);
        }

        return AjaxResult.success(miniProgramService.getUserTrainingStats(ctx.userId, ctx.tenantId));
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // Device / 设备
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * 扫码解析器械
     * GET /api/mini/equipment/resolve?code=EQ-000001
     */
    @GetMapping("/mini/equipment/resolve")
    public AjaxResult resolveEquipment(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestParam("code") String code,
            @RequestParam(value = "venueId", required = false) Long venueId) {
        if (StringUtils.isEmpty(code)) {
            return AjaxResult.error("器械编码不能为空");
        }

        AuthContext ctx = resolveAuth(auth);
        // 允许匿名访问，使用默认租户
        if (ctx == null) {
            ctx = new AuthContext();
            ctx.userId = null;
            ctx.tenantId = DEMO_TENANT_ID; // 使用默认租户 ID
            ctx.isDemo = false;
        }

        if (ctx.isDemo) {
            Map<String, Object> demo = new LinkedHashMap<>();
            demo.put("equipmentId", 1L);
            demo.put("equipmentCode", "EQ-000001");
            demo.put("equipmentName", "坐姿推胸训练器");
            demo.put("equipmentType", "chest_press");
            demo.put("location", "A区-01");
            demo.put("deviceId", 1L);
            demo.put("deviceCode", "HB-3412");
            demo.put("bluetoothName", "gy_ble25t1");
            demo.put("serviceUuid", "0000FFE0-0000-1000-8000-00805F9B34FB");
            demo.put("notifyCharUuid", "0000FFE4-0000-1000-8000-00805F9B34FB");

            Map<String, Object> countingConfig = new LinkedHashMap<>();
            countingConfig.put("mainAxis", "pitch");
            countingConfig.put("upThreshold", 20.0);
            countingConfig.put("downThreshold", 5.0);
            countingConfig.put("minIntervalMs", 600);
            countingConfig.put("minRange", 15.0);
            countingConfig.put("smoothingWindow", 5);
            demo.put("countingConfig", countingConfig);
            return AjaxResult.success(demo);
        }

        try {
            Long qrVenueId = venueId != null ? venueId : extractLongParam(code, "venueId", "venue_id", "groupId", "group_id");
            EquipmentResolveResult result = miniEquipmentService.resolveEquipment(code, ctx.tenantId);
            if (result == null) {
                return AjaxResult.error("未找到器械或器械未绑定 IMU");
            }
            if (qrVenueId != null) {
                if (!isEquipmentInVenue(qrVenueId, result.getDeviceId())) {
                    return AjaxResult.error("该器械不属于二维码指定场馆，请联系管理员检查二维码");
                }
                result.setVenueId(qrVenueId);
                result.setVenueName(resolveVenueName(qrVenueId, ctx.tenantId));
            }
            return AjaxResult.success(result);
        } catch (Exception e) {
            log.error("resolveEquipment failed: code={}, tenantId={}: {}", code, ctx.tenantId, e.getMessage());
            return AjaxResult.error("器械解析失败");
        }
    }

    /**
     * 占用器械后才允许小程序连接该器械 BLE 传感器。
     * POST /api/training/equipment/occupy
     */
    @PostMapping("/training/equipment/occupy")
    public AjaxResult occupyEquipment(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestBody Map<String, Object> body) {
        AuthContext ctx = resolveAuth(auth);
        if (ctx == null) return AjaxResult.error(401, "未登录");

        String equipmentCode = stringValue(firstPresent(body, "equipmentCode", "equipment_code"), null);
        Long venueId = longValue(firstPresent(body, "venueId", "venue_id"), null);
        if (StringUtils.isEmpty(equipmentCode)) {
            return AjaxResult.error("器械编号不能为空");
        }

        if (ctx.isDemo) {
            Map<String, Object> demo = new LinkedHashMap<>();
            demo.put("usageSessionId", "demo-" + System.currentTimeMillis());
            demo.put("status", "occupied");
            demo.put("expiresInSeconds", EQUIPMENT_OCCUPANCY_TTL_SECONDS);
            return AjaxResult.success("器械已占用", demo);
        }

        EquipmentResolveResult equipment = miniEquipmentService.resolveEquipment(equipmentCode, ctx.tenantId);
        if (equipment == null || equipment.getDeviceId() == null || StringUtils.isEmpty(equipment.getDeviceCode())) {
            return AjaxResult.error("器械不存在或未绑定蓝牙传感器");
        }
        if (venueId != null && !isEquipmentInVenue(venueId, equipment.getDeviceId())) {
            return AjaxResult.error("该器械不属于当前场馆，请重新扫码");
        }

        String key = occupancyKey(ctx.tenantId, equipment.getEquipmentCode());
        Map<String, Object> existing = redisCache.getCacheObject(key);
        if (isOwnedBy(existing, ctx.userId)) {
            existing.put("heartbeatAt", System.currentTimeMillis());
            redisCache.setCacheObject(key, existing, EQUIPMENT_OCCUPANCY_TTL_SECONDS, TimeUnit.SECONDS);
            return AjaxResult.success("器械占用已续期", buildOccupancyResponse(existing, equipment, venueId));
        }
        if (existing != null) {
            return AjaxResult.error(409, "器械正在使用中，请稍后再试");
        }

        Map<String, Object> occupancy = new LinkedHashMap<>();
        occupancy.put("usageSessionId", UUID.randomUUID().toString());
        occupancy.put("tenantId", ctx.tenantId);
        occupancy.put("userId", ctx.userId);
        occupancy.put("venueId", venueId);
        occupancy.put("equipmentCode", equipment.getEquipmentCode());
        occupancy.put("equipmentName", equipment.getEquipmentName());
        occupancy.put("deviceId", equipment.getDeviceId());
        occupancy.put("deviceCode", equipment.getDeviceCode());
        occupancy.put("bluetoothName", equipment.getBluetoothName());
        occupancy.put("startedAt", System.currentTimeMillis());
        occupancy.put("heartbeatAt", System.currentTimeMillis());

        Boolean locked = redisCache.redisTemplate.opsForValue()
                .setIfAbsent(key, occupancy, EQUIPMENT_OCCUPANCY_TTL_SECONDS, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            return AjaxResult.error(409, "器械正在使用中，请稍后再试");
        }
        return AjaxResult.success("器械已占用", buildOccupancyResponse(occupancy, equipment, venueId));
    }

    @PostMapping("/training/equipment/heartbeat")
    public AjaxResult heartbeatEquipment(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestBody Map<String, Object> body) {
        AuthContext ctx = resolveAuth(auth);
        if (ctx == null) return AjaxResult.error(401, "未登录");
        if (ctx.isDemo) return AjaxResult.success("心跳成功", Map.of("status", "active"));

        String equipmentCode = stringValue(firstPresent(body, "equipmentCode", "equipment_code"), null);
        String usageSessionId = stringValue(firstPresent(body, "usageSessionId", "usage_session_id"), null);
        Map<String, Object> occupancy = getOwnedOccupancy(ctx, equipmentCode, usageSessionId);
        if (occupancy == null) {
            return AjaxResult.error(409, "器械占用已失效，请重新扫码连接");
        }
        occupancy.put("heartbeatAt", System.currentTimeMillis());
        redisCache.setCacheObject(occupancyKey(ctx.tenantId, equipmentCode), occupancy,
                EQUIPMENT_OCCUPANCY_TTL_SECONDS, TimeUnit.SECONDS);
        return AjaxResult.success("心跳成功", Map.of("status", "active", "expiresInSeconds", EQUIPMENT_OCCUPANCY_TTL_SECONDS));
    }

    @PostMapping("/training/equipment/release")
    public AjaxResult releaseEquipment(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestBody Map<String, Object> body) {
        AuthContext ctx = resolveAuth(auth);
        if (ctx == null) return AjaxResult.error(401, "未登录");
        if (ctx.isDemo) return AjaxResult.success("器械已释放", Map.of("status", "released"));

        String equipmentCode = stringValue(firstPresent(body, "equipmentCode", "equipment_code"), null);
        String usageSessionId = stringValue(firstPresent(body, "usageSessionId", "usage_session_id"), null);
        Map<String, Object> occupancy = getOwnedOccupancy(ctx, equipmentCode, usageSessionId);
        if (occupancy != null) {
            redisCache.deleteObject(occupancyKey(ctx.tenantId, equipmentCode));
        }
        return AjaxResult.success("器械已释放", Map.of("status", "released"));
    }

    /**
     * 获取我的设备列表
     * GET /api/device/my
     *
     * <p>从 iot_device_binding 表查询用户有效绑定设备
     */
    @GetMapping("/device/my")
    public AjaxResult getMyDevices(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        AuthContext ctx = resolveAuth(auth);

        // 如果没有 token，返回 401
        if (ctx == null) {
            return AjaxResult.error(401, "未登录");
        }

        if (ctx.isDemo) {
            List<Map<String, Object>> devices = new ArrayList<>();
            Map<String, Object> device1 = new LinkedHashMap<>();
            device1.put("bindingId", 1);
            device1.put("deviceId", 1);
            device1.put("deviceCode", "HB-3412");
            device1.put("deviceName", "智能力量站 Pro");
            device1.put("firmwareVersion", "v1.2.0");
            device1.put("status", "online");
            devices.add(device1);
            return AjaxResult.success(devices);
        }

        try {
            List<Map<String, Object>> devices = miniProgramService.getUserDevices(ctx.userId, ctx.tenantId);
            log.info("getMyDevices: userId={}, tenantId={} → {} devices",
                     ctx.userId, ctx.tenantId, devices.size());
            return AjaxResult.success(devices);
        } catch (Exception e) {
            log.error("getMyDevices failed for userId={}: {}", ctx.userId, e.getMessage());
            return AjaxResult.success(Collections.emptyList());
        }
    }

    /**
     * 获取当前场馆/智能力量站
     * GET /api/venue/current
     *
     * <p>小程序首页展示的是场馆，不是单台蓝牙设备。当前复用后台「设备分组」作为场馆管理模型。
     */
    @GetMapping("/venue/current")
    public AjaxResult getCurrentVenue(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        AuthContext ctx = resolveAuth(auth);
        if (ctx == null) {
            return AjaxResult.error(401, "未登录");
        }

        if (ctx.isDemo) {
            Map<String, Object> venue = new LinkedHashMap<>();
            venue.put("venueId", 1);
            venue.put("venueName", "智能力量站");
            venue.put("description", "演示场馆");
            venue.put("deviceCount", 1);
            venue.put("status", "open");
            return AjaxResult.success(venue);
        }

        try {
            DeviceGroup query = new DeviceGroup();
            query.setTenantId(ctx.tenantId);
            List<DeviceGroup> groups = deviceGroupService.selectDeviceGroupList(query);
            if (groups == null || groups.isEmpty()) {
                Map<String, Object> fallback = new LinkedHashMap<>();
                fallback.put("venueId", null);
                fallback.put("venueName", "智能力量站");
                fallback.put("description", "请在后台设备分组中维护场馆");
                fallback.put("deviceCount", 0);
                fallback.put("status", "pending");
                return AjaxResult.success(fallback);
            }

            DeviceGroup group = groups.get(0);
            Map<String, Object> venue = new LinkedHashMap<>();
            venue.put("venueId", group.getGroupId());
            venue.put("venueName", group.getGroupName());
            venue.put("manufacturerName", group.getManufacturerName());
            venue.put("description", group.getDescription());
            venue.put("deviceCount", group.getDeviceCount());
            venue.put("status", "open");
            return AjaxResult.success(venue);
        } catch (Exception e) {
            log.error("getCurrentVenue failed for userId={}: {}", ctx.userId, e.getMessage());
            return AjaxResult.error("场馆加载失败");
        }
    }

    /**
     * 获取可选择场馆列表
     * GET /api/venue/list
     */
    @GetMapping("/venue/list")
    public AjaxResult listVenues(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        AuthContext ctx = resolveAuth(auth);
        if (ctx == null) {
            return AjaxResult.error(401, "未登录");
        }

        if (ctx.isDemo) {
            List<Map<String, Object>> venues = new ArrayList<>();
            Map<String, Object> venue = new LinkedHashMap<>();
            venue.put("venueId", 1);
            venue.put("venueName", "智能力量站");
            venue.put("description", "演示场馆");
            venue.put("deviceCount", 1);
            venue.put("status", "open");
            venues.add(venue);
            return AjaxResult.success(venues);
        }

        try {
            DeviceGroup query = new DeviceGroup();
            query.setTenantId(ctx.tenantId);
            List<DeviceGroup> groups = deviceGroupService.selectDeviceGroupList(query);
            List<Map<String, Object>> venues = new ArrayList<>();
            for (DeviceGroup group : groups) {
                Map<String, Object> venue = new LinkedHashMap<>();
                venue.put("venueId", group.getGroupId());
                venue.put("venueName", group.getGroupName());
                venue.put("manufacturerName", group.getManufacturerName());
                venue.put("description", group.getDescription());
                venue.put("deviceCount", group.getDeviceCount());
                venue.put("status", "open");
                venues.add(venue);
            }
            return AjaxResult.success(venues);
        } catch (Exception e) {
            log.error("listVenues failed for userId={}: {}", ctx.userId, e.getMessage());
            return AjaxResult.error("场馆列表加载失败");
        }
    }

    /**
     * 获取场馆可扫码训练器械
     * GET /api/venue/{venueId}/equipment
     */
    @GetMapping("/venue/{venueId}/equipment")
    public AjaxResult listVenueEquipment(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable Long venueId) {
        AuthContext ctx = resolveAuth(auth);
        if (ctx == null) {
            return AjaxResult.error(401, "未登录");
        }
        if (ctx.isDemo) {
            return AjaxResult.success(demoVenueEquipment());
        }
        try {
            return AjaxResult.success(getVenueEquipmentList(venueId, ctx.tenantId));
        } catch (Exception e) {
            log.error("listVenueEquipment failed: venueId={}, userId={}: {}", venueId, ctx.userId, e.getMessage());
            return AjaxResult.error("场馆器械加载失败");
        }
    }

    /**
     * 绑定设备
     * POST /api/device/bind
     * Body: { deviceCode: string, deviceName?: string }
     *
     * <p>写入 iot_device_binding 表，支持重复绑定检测
     */
    @PostMapping("/device/bind")
    public AjaxResult bindDevice(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestBody Map<String, String> body) {
        AuthContext ctx = resolveAuth(auth);
        if (ctx == null) return AjaxResult.error(401, "未登录");

        String deviceCode = body.get("deviceCode");
        if (StringUtils.isEmpty(deviceCode)) {
            return AjaxResult.error("设备编号不能为空");
        }
        String deviceName = body.get("deviceName");

        if (ctx.isDemo) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("bindingId", System.currentTimeMillis());
            result.put("deviceCode", deviceCode);
            result.put("deviceName", StringUtils.isEmpty(deviceName) ? deviceCode : deviceName);
            result.put("status", "active");
            return AjaxResult.success("设备绑定成功", result);
        }

        try {
            Map<String, Object> result = miniProgramService.bindDevice(
                    ctx.userId, ctx.tenantId, deviceCode, deviceName);
            if (Boolean.TRUE.equals(result.get("success"))) {
                return AjaxResult.success(result.get("msg").toString(), result);
            } else {
                return AjaxResult.error(result.get("msg").toString());
            }
        } catch (Exception e) {
            log.error("bindDevice failed: userId={}, deviceCode={}: {}",
                     ctx.userId, deviceCode, e.getMessage());
            return AjaxResult.error("设备绑定失败: " + e.getMessage());
        }
    }

    /**
     * 解绑设备
     * DELETE /api/device/unbind/{bindingId}
     */
    @DeleteMapping("/device/unbind/{bindingId}")
    public AjaxResult unbindDevice(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable Long bindingId) {
        AuthContext ctx = resolveAuth(auth);
        if (ctx == null) return AjaxResult.error(401, "未登录");

        if (ctx.isDemo) {
            return AjaxResult.success("设备已解绑", null);
        }

        try {
            boolean ok = miniProgramService.unbindDevice(ctx.userId, ctx.tenantId, bindingId);
            if (ok) {
                log.info("unbindDevice: userId={}, tenantId={}, bindingId={}", ctx.userId, ctx.tenantId, bindingId);
                return AjaxResult.success("设备已解绑", null);
            } else {
                return AjaxResult.error("解绑失败：无权操作或设备不存在");
            }
        } catch (Exception e) {
            log.error("unbindDevice failed: bindingId={}: {}", bindingId, e.getMessage());
            return AjaxResult.error("解绑失败");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // Training Data / 训练数据
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * 今日训练进度
     * GET /api/training/progress/today
     *
     * <p>优先使用 interv_session 表实时数据，失败时返回 B Line 本地降级数据。
     */
    @GetMapping("/training/progress/today")
    public AjaxResult getTodayProgress(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        AuthContext ctx = resolveAuth(auth);

        // 如果没有 token，返回 401
        if (ctx == null) {
            return AjaxResult.error(401, "未登录");
        }

        if (ctx.isDemo) {
            Map<String, Object> progress = new LinkedHashMap<>();
            progress.put("completedSessions", 1);
            progress.put("plannedSessions", 4);
            progress.put("totalDurationMin", 45);
            progress.put("totalReps", 10);
            progress.put("complianceRate", 72);
            return AjaxResult.success(progress);
        }

        try {
            // 优先从 interv_session 实时计算
            Map<String, Object> progress = miniProgramService.getTodayProgress(ctx.userId, ctx.tenantId);
            return AjaxResult.success(progress);
        } catch (Exception e) {
            log.warn("getTodayProgress: miniProgramService failed for userId={}, fallback to local defaults",
                     ctx.userId, e.getMessage());
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("completedSessions", 0);
            fallback.put("plannedSessions", 4);
            fallback.put("totalDurationMin", 0);
            fallback.put("totalReps", 0);
            fallback.put("complianceRate", 0);
            return AjaxResult.success(fallback);
        }
    }

    /**
     * 训练历史
     * GET /api/training/history?page=1&size=10
     *
     * <p>从 interv_session 表分页查询
     */
    @GetMapping("/training/history")
    public AjaxResult getTrainingHistory(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        AuthContext ctx = resolveAuth(auth);
        if (ctx == null) return AjaxResult.error(401, "未登录");

        if (ctx.isDemo) {
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("records", Collections.emptyList());
            fallback.put("total", 0);
            fallback.put("page", page);
            fallback.put("size", size);
            fallback.put("userId", DEMO_USER_ID);
            return AjaxResult.success(fallback);
        }

        try {
            Map<String, Object> result = miniProgramService.getTrainingHistory(
                    ctx.userId, ctx.tenantId, page, size);
            return AjaxResult.success(result);
        } catch (Exception e) {
            log.error("getTrainingHistory failed: userId={}: {}", ctx.userId, e.getMessage());
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("records", Collections.emptyList());
            fallback.put("total", 0);
            fallback.put("page", page);
            fallback.put("size", size);
            return AjaxResult.success(fallback);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // Intervention Prescriptions / 干预处方 (XIN-147 核心)
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * 获取小程序运动处方 (AI个性化方案)
     * POST /api/training/prescription
     *
     * <p>由 B Line 后端生成或读取后台分配计划，不依赖未提供的 A Line API。
     * 串联: 小程序 → RuoYi-Backend → B Line DB/规则处方
     *
     * <p>多租户: tenantId 通过 Authorization token 解析，并在 B Line 内部隔离。
     */
    @PostMapping("/training/prescription")
    public AjaxResult getPrescription(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestBody Map<String, Object> body) {
        AuthContext ctx = resolveAuth(auth);
        if (ctx == null) return AjaxResult.error(401, "未登录");

        String deviceType = stringValue(firstPresent(body, "deviceType", "device_type"), "strength_station");
        Long venueId = longValue(firstPresent(body, "venueId", "venue_id"), null);

        int age = ctx.userInfo != null && ctx.userInfo.get("age") != null
                ? (Integer) ctx.userInfo.get("age") : 30;

        log.info("getPrescription: rule-generated userId={}, tenantId={}, deviceType={}, venueId={}",
                 ctx.getUserIdStr(), ctx.tenantId, deviceType, venueId);

        if (ctx.isDemo) {
            return AjaxResult.success(applyVenueEquipmentTasks(
                    buildFallbackPrescription(DEMO_USER_ID, deviceType, age), demoVenueEquipment(), age));
        }

        Map<String, Object> prescription = buildAdminTrainingPlan(ctx.userId, ctx.tenantId);
        boolean usesAdminPlan = prescription != null;
        if (prescription == null) {
            prescription = miniProgramService.buildRulePrescription(
                    ctx.userId, ctx.tenantId, deviceType, age);
        }
        if (venueId != null && !usesAdminPlan) {
            List<Map<String, Object>> equipment = getVenueEquipmentList(venueId, ctx.tenantId);
            if (!equipment.isEmpty()) {
                prescription = applyVenueEquipmentTasks(prescription, equipment, age);
            }
        }
        return AjaxResult.success(prescription);
    }

    /**
     * 获取指定设备类型的训练任务模板
     * POST /api/mini/device-tasks
     *
     * <p>兼容早期小程序 device-tasks API；当前由 B Line/RuoYi 本地规则生成，
     * 不再直连独立 intervention-engine 服务。
     */
    @PostMapping("/mini/device-tasks")
    public AjaxResult getMiniDeviceTasks(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestBody Map<String, Object> body) {
        AuthContext ctx = resolveAuth(auth);
        if (ctx == null) return AjaxResult.error(401, "未登录");

        String deviceType = stringValue(firstPresent(body, "device_type", "deviceType"), "strength_station");
        int age = longValue(firstPresent(body, "age"), 30L).intValue();

        List<Map<String, Object>> tasks = new ArrayList<>();
        if ("body_composition".equals(deviceType) || "body_fat_scale".equals(deviceType) || "scale".equals(deviceType)) {
            tasks.add(deviceExerciseTask(1, "身体成分测量", "body_composition", 1, 1, 0.0, 30,
                    "低", "站稳后完成一次测量，作为身体引擎后续输入。"));
        } else if ("treadmill".equals(deviceType) || "rowing".equals(deviceType) || "cycling".equals(deviceType)) {
            tasks.add(deviceExerciseTask(1, "低强度热身", "warmup", 1, 5, 0.0, 60,
                    "低", "先完成5分钟热身，观察主观疲劳。"));
            tasks.add(deviceExerciseTask(2, cardioExerciseName(deviceType), deviceType, 1, age >= 60 ? 10 : 15, 0.0, 90,
                    age >= 60 ? "低中" : "中等", "保持可对话强度，不追求速度。"));
        } else {
            tasks.add(deviceExerciseTask(1, getExerciseNameByDevice(deviceType), getExerciseType(deviceType),
                    age >= 60 ? 2 : 3, age >= 60 ? 10 : 12, defaultLoadForEquipment(deviceType),
                    age >= 60 ? 90 : 75, age >= 60 ? "低中" : "中等", "扫码器械二维码后开始，保持动作轨迹稳定。"));
            tasks.add(deviceExerciseTask(2, "辅助稳定训练", "stability",
                    2, 10, 0.0, 60, "低", "动作放慢，优先保证完整幅度和稳定呼吸。"));
            tasks.add(deviceExerciseTask(3, "放松拉伸", "mobility",
                    1, 6, 0.0, 45, "低", "训练后做轻量拉伸，记录疼痛或不适。"));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("device_type", deviceType);
        result.put("tasks", tasks);
        result.put("total_exercises", tasks.size());
        result.put("muscle_groups", muscleGroupsForDevice(deviceType));
        result.put("source", "ruoyi_b_line");
        result.put("tenantId", ctx.tenantId);
        return AjaxResult.success(result);
    }

    /**
     * 提交训练完成记录
     * POST /api/training/session
     *
     * <p>XIN-147: 将完成的训练记录写入 interv_session 表
     */
    @PostMapping("/training/session")
    public AjaxResult submitSession(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestBody Map<String, Object> body) {
        AuthContext ctx = resolveAuth(auth);
        if (ctx == null) return AjaxResult.error(401, "未登录");

        String exerciseType = body.get("exerciseType") != null
                ? (String) body.get("exerciseType") : "strength";
        Integer completedSets = body.get("completedSets") != null
                ? ((Number) body.get("completedSets")).intValue() : 0;
        Integer totalReps = body.get("totalReps") != null
                ? ((Number) body.get("totalReps")).intValue() : 0;
        Integer durationMin = body.get("durationMin") != null
                ? ((Number) body.get("durationMin")).intValue() : 0;
        Double totalVolume = body.get("totalVolumeKg") != null
                ? ((Number) body.get("totalVolumeKg")).doubleValue() : 0.0;
        String stage = body.get("stage") != null ? (String) body.get("stage") : null;
        String equipmentCode = body.get("equipmentCode") != null ? (String) body.get("equipmentCode") : null;
        String deviceCode = body.get("deviceCode") != null ? (String) body.get("deviceCode") : null;
        String usageSessionId = body.get("usageSessionId") != null ? String.valueOf(body.get("usageSessionId")) : null;
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sets = body.get("sets") instanceof List
                ? (List<Map<String, Object>>) body.get("sets") : Collections.emptyList();

        if (ctx.isDemo) {
            log.warn("submitSession [demo]: type={}, sets={}, reps={}, duration={}",
                     exerciseType, completedSets, totalReps, durationMin);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("sessionId", System.currentTimeMillis());
            result.put("status", "recorded");
            result.put("savedSets", sets.size());
            result.put("fallback", true);
            result.put("userId", DEMO_USER_ID);
            attachAlineAnalysis(result, ctx, body, exerciseType, completedSets, totalReps,
                    totalVolume, durationMin, equipmentCode, deviceCode, stage, sets);
            return AjaxResult.success("训练记录已保存", result);
        }

        try {
            Long occupiedDeviceId = null;
            if (!StringUtils.isEmpty(equipmentCode)) {
                Map<String, Object> occupancy = getOwnedOccupancy(ctx, equipmentCode, usageSessionId);
                if (occupancy == null) {
                    return AjaxResult.error(409, "器械占用已失效，请重新扫码连接");
                }
                occupiedDeviceId = longValue(occupancy.get("deviceId"), null);
                String occupiedDeviceCode = stringValue(occupancy.get("deviceCode"), null);
                if (!StringUtils.isEmpty(occupiedDeviceCode) && !StringUtils.isEmpty(deviceCode)
                        && !occupiedDeviceCode.equals(deviceCode)) {
                    return AjaxResult.error("训练传感器与占用器械不一致，请重新扫码");
                }
            }
            Map<String, Object> result = miniProgramService.submitSession(
                    ctx.userId, ctx.tenantId, occupiedDeviceId,
                    equipmentCode, deviceCode, exerciseType,
                    completedSets, totalReps, totalVolume, durationMin, stage, sets);
            attachAlineAnalysis(result, ctx, body, exerciseType, completedSets, totalReps,
                    totalVolume, durationMin, equipmentCode, deviceCode, stage, sets);
            if (!StringUtils.isEmpty(equipmentCode)) {
                redisCache.deleteObject(occupancyKey(ctx.tenantId, equipmentCode));
            }
            log.info("submitSession: userId={}, tenantId={}, type={}, sets={}, reps={}",
                     ctx.userId, ctx.tenantId, exerciseType, completedSets, totalReps);
            return AjaxResult.success("训练记录已保存", result);
        } catch (Exception e) {
            log.error("submitSession failed: userId={}: {}", ctx.userId, e.getMessage());
            if (ctx.isDemo) {
                log.warn("submitSession [demo fallback]: type={}, sets={}, reps={}, duration={}",
                         exerciseType, completedSets, totalReps, durationMin);
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("sessionId", System.currentTimeMillis());
                result.put("status", "recorded");
                result.put("savedSets", sets.size());
                result.put("fallback", true);
                attachAlineAnalysis(result, ctx, body, exerciseType, completedSets, totalReps,
                        totalVolume, durationMin, equipmentCode, deviceCode, stage, sets);
                return AjaxResult.success("训练记录已保存", result);
            }
            return AjaxResult.error("训练记录保存失败: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // A Line Placeholder / B Line Analysis Gateway
    // ═══════════════════════════════════════════════════════════════════════════════

    private void attachAlineAnalysis(Map<String, Object> result, AuthContext ctx, Map<String, Object> body,
            String exerciseType, Integer completedSets, Integer totalReps, Double totalVolume,
            Integer durationMin, String equipmentCode, String deviceCode, String stage,
            List<Map<String, Object>> sets) {
        try {
            AlineAnalysisRequest request = new AlineAnalysisRequest();
            request.setTenantId(ctx.tenantId);
            request.setUserId(ctx.isDemo ? DEMO_USER_ID : ctx.getUserIdStr());
            request.setSessionId(stringValue(result.get("sessionId"), null));
            request.setTaskId(stringValue(firstPresent(body, "taskId", "task_id"), null));
            request.setPrescriptionId(stringValue(firstPresent(body, "prescriptionId", "prescription_id"), null));
            request.setEquipmentCode(equipmentCode);
            request.setDeviceCode(deviceCode);
            request.setExerciseType(exerciseType);
            request.setCompletedSets(completedSets);
            request.setTotalReps(totalReps);
            request.setDurationMin(durationMin);
            request.setTotalVolumeKg(totalVolume);
            request.setSets(sets);
            request.setSource("b_line");

            AlineAnalysisResult analysis = alineAnalysisGateway.createAnalysis(request);
            result.put("analysisTaskId", analysis.getTaskId());
            result.put("analysisStatus", analysis.getStatus());
            result.put("analysisMessage", analysis.getMessage());
            result.put("aiFeedback", analysis.getAiFeedback());
            result.put("alineProvider", analysis.getProvider());
        } catch (Exception e) {
            log.warn("attachAlineAnalysis skipped: sessionId={}, error={}",
                    result != null ? result.get("sessionId") : null, e.getMessage());
            result.put("analysisStatus", "unavailable");
            result.put("analysisMessage", "A Line 分析占位生成失败，训练记录已保存。");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildAdminTrainingPlan(Long userId, Long tenantId) {
        IntervPrescription active = prescriptionMapper.selectLatestByUserId(String.valueOf(userId), tenantId);
        if (active == null || StringUtils.isEmpty(active.getRecommendations())) {
            return null;
        }
        try {
            Map<String, Object> source = JSON.parseObject(active.getRecommendations(), Map.class);
            Object tasks = source.get("tasks");
            if (!(tasks instanceof List<?>)) {
                return null;
            }
            int plannedSets = 0;
            for (Object item : (List<?>) tasks) {
                if (item instanceof Map<?, ?> task) {
                    Object targetSets = task.get("targetSets");
                    plannedSets += targetSets instanceof Number ? ((Number) targetSets).intValue() : 1;
                }
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("date", LocalDate.now().toString());
            result.put("plannedSessions", plannedSets);
            result.put("completedSessions", 0);
            result.put("totalDurationMin", 0);
            result.put("totalSets", plannedSets);
            result.put("totalReps", 0);
            result.put("complianceRate", 0);
            result.put("tasks", tasks);
            result.put("aiSuggestion", "已加载后台为你设定的训练计划，请按任务扫码对应器械开始。");
            result.put("coachingReasoning", "该计划由后台教练/管理员针对当前用户设定。");
            result.put("exerciseGoal", "个人训练计划");
            result.put("exerciseGoalEn", "admin_assigned_plan");
            result.put("userStage", "assigned");
            result.put("targetHrZone", null);
            result.put("healthTips", Collections.emptyList());
            result.put("algorithmVersion", "XIN-ADMIN-v1");
            result.put("prescriptionId", active.getPrescriptionId());
            result.put("generatedForUserId", String.valueOf(userId));
            result.put("source", "admin");
            return result;
        } catch (Exception e) {
            log.warn("buildAdminTrainingPlan failed: userId={}, prescriptionId={}, error={}",
                    userId, active.getPrescriptionId(), e.getMessage());
            return null;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // Auth Resolution
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Lightweight auth context resolved from Authorization header.
     *
     * <p>MVP strategy:
     * <ul>
     *   <li>Demo token ("mp_demo_token_2026") → demo user, no tenant</li>
     *   <li>Bearer JWT → parse JWT directly to get userId + tenantId</li>
     *   <li>Empty/missing → error (小程序必须登录)</li>
     * </ul>
     */
    private static class AuthContext {
        Long userId;
        Long tenantId;
        boolean isDemo;
        Map<String, Object> userInfo; // cached user info for demo mode

        String getUserIdStr() {
            return userId != null ? String.valueOf(userId) : null;
        }
    }

    private AuthContext resolveAuth(String auth) {
        if (StringUtils.isEmpty(auth)) {
            return null; // require auth
        }

        String token = auth.replaceFirst("^Bearer\\s+", "").trim();

        // Demo token
        if (DEMO_TOKEN.equals(token)) {
            AuthContext ctx = new AuthContext();
            // Demo mode is intentionally not tied to a real sys_user row.
            ctx.userId = null;
            ctx.tenantId = DEMO_TENANT_ID;
            ctx.isDemo = true;
            ctx.userInfo = DEMO_USERS.get(DEMO_TOKEN);
            return ctx;
        }

        // Real JWT token — parse directly to get userId + tenantId
        AuthContext ctx = parseJwtForAuth(token);
        if (ctx != null) {
            return ctx;
        }

        return null;
    }

    /**
     * Parse JWT token directly using jjwt (bypasses private parseToken method).
     * Extracts userId + tenantId from custom claims.
     */
    private AuthContext parseJwtForAuth(String token) {
        try {
            // RuoYi tokens use HS512 and store user info in Redis with the uuid as key.
            // The uuid is stored as the JWT's "user_key" claim (Constants.LOGIN_USER_KEY = "login_user:uuid")
            // We need to look up in Redis to get the full LoginUser.
            // Since Redis is instance-scoped, we parse the JWT without validation to get the uuid,
            // then query Redis.
            //
            // Alternative for simplicity in MVP: use TokenService.getLoginUser(HttpServletRequest)
            // by constructing a fake request. But that's messy.
            //
            // Best approach: parse JWT claims without signature validation for userId/tenantId,
            // since RuoYi tokens are self-contained enough for our needs.
            // Note: In production, you'd validate the signature properly.

            Claims claims = Jwts.parser()
                    .setSigningKey(jwtSecret)
                    .parseClaimsJws(token)
                    .getBody();

            String userKey = claims.get(Constants.LOGIN_USER_KEY, String.class);
            String username = claims.get(Constants.JWT_USERNAME, String.class);

            // Look up in Redis to get full LoginUser
            String uuid = null;
            if (userKey != null) {
                uuid = userKey.contains(":") ? userKey.split(":", 2)[1] : userKey;
            }

            if (uuid != null) {
                String redisKey = CacheConstants.LOGIN_TOKEN_KEY + uuid;
                LoginUser loginUser = redisCache.getCacheObject(redisKey);
                if (loginUser != null) {
                    AuthContext ctx = new AuthContext();
                    ctx.userId = loginUser.getUserId();
                    ctx.tenantId = loginUser.getTenantId();
                    ctx.isDemo = false;
                    ctx.userInfo = null;
                    return ctx;
                }
            }

            // Redis miss: extract from JWT claims directly
            // RuoYi stores userId as Long in the JWT subject or as a custom claim
            String subject = claims.getSubject();
            Long userId = null;
            Long tenantId = 1L;

            if (subject != null && subject.matches("\\d+")) {
                userId = Long.valueOf(subject);
            }

            // Try custom claim
            Object uidClaim = claims.get("userId");
            if (uidClaim != null) {
                userId = Long.valueOf(uidClaim.toString());
            }
            Object tidClaim = claims.get("tenantId");
            if (tidClaim != null) {
                tenantId = Long.valueOf(tidClaim.toString());
            }

            if (userId != null) {
                AuthContext ctx = new AuthContext();
                ctx.userId = userId;
                ctx.tenantId = tenantId;
                ctx.isDemo = false;
                ctx.userInfo = null;
                return ctx;
            }

        } catch (Exception e) {
            log.warn("parseJwtForAuth: failed for token: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Generate a RuoYi JWT token for a mini-program user.
     * Uses TokenService.createToken() which stores LoginUser in Redis.
     */
    private String generateMpToken(SysUser sysUser) {
        LoginUser loginUser = new LoginUser(sysUser, new java.util.HashSet<>());
        loginUser.setUserId(sysUser.getUserId());
        loginUser.setDeptId(sysUser.getDeptId());
        loginUser.setTenantId(sysUser.getTenantId());
        // TokenService.createToken() generates UUID token, stores in Redis
        String token = tokenService.createToken(loginUser);
        return token;
    }

    /**
     * Find or create a mini-program user.
     * Priority: wechat openid > phone > create new
     */
    private SysUser findOrCreateMpUser(String wxOpenId, String phone, String nickname, String avatar, String loginType) {
        try {
            if (!StringUtils.isEmpty(wxOpenId)) {
                String userName = buildWechatUserName(wxOpenId);
                SysUser existing = userService.selectUserByUserName(userName);
                if (existing != null) {
                    return syncMpUserProfile(existing, nickname, avatar);
                }
            }

            // Try to find by phone first
            if (!StringUtils.isEmpty(phone)) {
                SysUser query = new SysUser();
                query.setPhonenumber(phone);
                query.setTenantId(1L); // default tenant for now
                var list = userService.selectUserList(query);
                if (list != null && !list.isEmpty()) {
                    return syncMpUserProfile(list.get(0), nickname, avatar);
                }
            }

            // Create new user
            SysUser newUser = new SysUser();
            newUser.setUserName(!StringUtils.isEmpty(wxOpenId)
                    ? buildWechatUserName(wxOpenId)
                    : loginType + "_" + (!StringUtils.isEmpty(phone) ? phone : "unknown"));
            newUser.setNickName(!StringUtils.isEmpty(nickname) ? nickname
                    : (!StringUtils.isEmpty(phone) ? maskPhone(phone) : "用户"));
            newUser.setAvatar(sanitizeMpAvatar(avatar));
            newUser.setPhonenumber(phone);
            newUser.setTenantId(1L);
            newUser.setDeptId(100L);  // 100 = 昕动智能 root dept
            newUser.setPassword("mp_no_password_2026"); // marker — cannot login with password
            newUser.setStatus("0");
            newUser.setDelFlag("0");

            boolean ok = userService.registerUser(newUser);
            if (ok) {
                SysUser created = userService.selectUserByUserName(newUser.getUserName());
                log.info("findOrCreateMpUser: created userId={}, loginType={}", created.getUserId(), loginType);
                return created;
            }
        } catch (Exception e) {
            log.warn("findOrCreateMpUser failed: {} (DB may be unavailable)", e.getMessage());
        }
        return null;
    }

    private SysUser syncMpUserProfile(SysUser existing, String nickname, String avatar) {
        if (existing == null) {
            return null;
        }

        boolean changed = false;
        SysUser update = new SysUser();
        update.setUserId(existing.getUserId());
        update.setDeptId(0L);
        update.setUpdateBy("mini-program");

        if (!StringUtils.isEmpty(nickname) && !nickname.equals(existing.getNickName())) {
            update.setNickName(nickname);
            existing.setNickName(nickname);
            changed = true;
        }
        String cleanAvatar = sanitizeMpAvatar(avatar);
        if (!StringUtils.isEmpty(cleanAvatar) && !cleanAvatar.equals(existing.getAvatar())) {
            update.setAvatar(cleanAvatar);
            existing.setAvatar(cleanAvatar);
            changed = true;
        }

        if (changed) {
            userService.updateUserProfile(update);
        }
        return existing;
    }

    private WechatSession resolveWechatSession(String code) {
        if (StringUtils.isEmpty(wechatMiniAppId) || StringUtils.isEmpty(wechatMiniAppSecret)) {
            WechatSession session = new WechatSession();
            session.openid = "dev_" + Integer.toHexString(code.hashCode());
            session.devMode = true;
            log.warn("resolveWechatSession: wechat.miniapp.appid/secret not configured, using dev openid");
            return session;
        }

        String url = UriComponentsBuilder
                .fromUriString("https://api.weixin.qq.com/sns/jscode2session")
                .queryParam("appid", wechatMiniAppId)
                .queryParam("secret", wechatMiniAppSecret)
                .queryParam("js_code", code)
                .queryParam("grant_type", "authorization_code")
                .toUriString();
        String response = restTemplate.getForObject(url, String.class);
        JSONObject json = JSONObject.parseObject(response);
        if (json == null || json.getString("openid") == null) {
            log.warn("resolveWechatSession: jscode2session failed: {}", response);
            return null;
        }

        WechatSession session = new WechatSession();
        session.openid = json.getString("openid");
        session.sessionKey = json.getString("session_key");
        session.unionid = json.getString("unionid");
        return session;
    }

    private String buildWechatUserName(String openid) {
        return "mp_wx_" + Integer.toHexString(openid.hashCode());
    }

    private String maskOpenid(String openid) {
        if (StringUtils.isEmpty(openid) || openid.length() <= 8) {
            return openid;
        }
        return openid.substring(0, 4) + "****" + openid.substring(openid.length() - 4);
    }

    private String sanitizeMpAvatar(String avatar) {
        if (StringUtils.isEmpty(avatar)) {
            return null;
        }
        String value = avatar.trim();
        if (value.isEmpty()) {
            return null;
        }
        if (value.length() > MP_AVATAR_MAX_LENGTH) {
            return value.substring(0, MP_AVATAR_MAX_LENGTH);
        }
        return value;
    }

    private static class WechatSession {
        private String openid;
        private String sessionKey;
        private String unionid;
        private boolean devMode;
    }

    private Map<String, Object> buildMpUserInfo(SysUser u) {
        Map<String, Object> info = new LinkedHashMap<>();
        String nickname = u.getNickName();
        boolean hasCustomNickname = hasCustomNickname(nickname);
        info.put("userId", String.valueOf(u.getUserId()));
        info.put("tenantId", u.getTenantId());
        info.put("nickname", hasCustomNickname ? nickname : "");
        info.put("needsProfileSetup", !hasCustomNickname);
        info.put("avatar", u.getAvatar() != null ? u.getAvatar() : "");
        info.put("phone", !StringUtils.isEmpty(u.getPhonenumber()) ? maskPhone(u.getPhonenumber()) : "");
        info.put("level", 1);
        info.put("streakDays", 0);
        info.put("complianceRate", 0);
        info.put("totalSessions", 0);
        info.put("stage", "beginner");
        info.put("stageLabel", "初学期");
        // TODO: Load health profile from RuoYi body-engine adapter when A Line API is ready.
        info.put("age", 30);
        info.put("gender", "未指定");
        info.put("deviceType", "strength_station");
        return info;
    }

    private boolean hasCustomNickname(String nickname) {
        if (StringUtils.isEmpty(nickname)) {
            return false;
        }
        String value = nickname.trim();
        return !"微信用户".equals(value)
                && !"用户".equals(value)
                && !"å¾®ä¿¡ç”¨æˆ·".equals(value);
    }

    private String maskPhone(String phone) {
        if (StringUtils.isEmpty(phone) || phone.length() != 11) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    private Object firstPresent(Map<String, Object> body, String... keys) {
        if (body == null) return null;
        for (String key : keys) {
            Object value = body.get(key);
            if (value != null && !String.valueOf(value).isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String stringValue(Object value, String fallback) {
        return value != null && !String.valueOf(value).isBlank() ? String.valueOf(value) : fallback;
    }

    private Long longValue(Object value, Long fallback) {
        if (value == null || String.valueOf(value).isBlank()) {
            return fallback;
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private Long extractLongParam(String raw, String... keys) {
        if (StringUtils.isEmpty(raw)) {
            return null;
        }
        String value = raw.trim();
        try {
            value = java.net.URLDecoder.decode(value, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
        String lower = value.toLowerCase();
        for (String key : keys) {
            String needle = key.toLowerCase() + "=";
            int idx = lower.indexOf(needle);
            if (idx < 0) {
                continue;
            }
            String found = value.substring(idx + needle.length());
            int amp = found.indexOf('&');
            if (amp >= 0) {
                found = found.substring(0, amp);
            }
            int hash = found.indexOf('#');
            if (hash >= 0) {
                found = found.substring(0, hash);
            }
            return longValue(found, null);
        }
        return null;
    }

    private String occupancyKey(Long tenantId, String equipmentCode) {
        return EQUIPMENT_OCCUPANCY_PREFIX + tenantId + ":" + equipmentCode;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getOwnedOccupancy(AuthContext ctx, String equipmentCode, String usageSessionId) {
        if (StringUtils.isEmpty(equipmentCode)) {
            return null;
        }
        Map<String, Object> occupancy = redisCache.getCacheObject(occupancyKey(ctx.tenantId, equipmentCode));
        if (!isOwnedBy(occupancy, ctx.userId)) {
            return null;
        }
        String currentSessionId = stringValue(occupancy.get("usageSessionId"), null);
        if (!StringUtils.isEmpty(usageSessionId) && !usageSessionId.equals(currentSessionId)) {
            return null;
        }
        return occupancy;
    }

    private boolean isOwnedBy(Map<String, Object> occupancy, Long userId) {
        if (occupancy == null || userId == null || occupancy.get("userId") == null) {
            return false;
        }
        return String.valueOf(userId).equals(String.valueOf(occupancy.get("userId")));
    }

    private Map<String, Object> buildOccupancyResponse(Map<String, Object> occupancy,
                                                       EquipmentResolveResult equipment,
                                                       Long venueId) {
        Map<String, Object> result = new LinkedHashMap<>(occupancy);
        result.put("status", "occupied");
        result.put("expiresInSeconds", EQUIPMENT_OCCUPANCY_TTL_SECONDS);
        result.put("venueId", venueId);
        result.put("equipment", equipment);
        return result;
    }

    private boolean isEquipmentInVenue(Long venueId, Long deviceId) {
        if (venueId == null || deviceId == null) {
            return false;
        }
        List<Long> deviceIds = deviceGroupService.getDeviceIdsByGroupId(venueId);
        return deviceIds != null && deviceIds.contains(deviceId);
    }

    private String resolveVenueName(Long venueId, Long tenantId) {
        if (venueId == null) {
            return null;
        }
        TenantContextHolder.setTenantId(tenantId);
        try {
            DeviceGroup group = deviceGroupService.selectDeviceGroupById(venueId);
            return group != null ? group.getGroupName() : null;
        } catch (Exception ignored) {
            return null;
        } finally {
            TenantContextHolder.clear();
        }
    }

    private List<Map<String, Object>> getVenueEquipmentList(Long venueId, Long tenantId) {
        List<Long> deviceIds = deviceGroupService.getDeviceIdsByGroupId(venueId);
        if (deviceIds == null || deviceIds.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> deviceIdSet = new HashSet<>(deviceIds);
        Equipment query = new Equipment();
        query.setStatus("0");
        List<Equipment> allEquipment;
        TenantContextHolder.setTenantId(tenantId);
        try {
            allEquipment = equipmentService.selectEquipmentList(query);
        } finally {
            TenantContextHolder.clear();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Equipment equipment : allEquipment) {
            if (equipment.getDeviceId() == null || !deviceIdSet.contains(equipment.getDeviceId())) {
                continue;
            }
            result.add(equipmentPayload(equipment));
        }
        return result;
    }

    private Map<String, Object> equipmentPayload(Equipment equipment) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("equipmentId", equipment.getEquipmentId());
        item.put("equipmentCode", equipment.getEquipmentCode());
        item.put("equipmentName", equipment.getEquipmentName());
        item.put("equipmentType", equipment.getEquipmentType());
        item.put("equipmentCategory", equipmentCategory(equipment.getEquipmentType()));
        item.put("deviceId", equipment.getDeviceId());
        item.put("deviceCode", equipment.getDeviceCode());
        item.put("bluetoothName", equipment.getBluetoothName());
        return item;
    }

    private List<Map<String, Object>> demoVenueEquipment() {
        List<Map<String, Object>> equipment = new ArrayList<>();
        equipment.add(demoEquipment(1L, "EQ-000001", "推胸训练器", "chest_press", "HB-3412", "gy_ble25t1"));
        equipment.add(demoEquipment(2L, "EQ-000002", "上斜训练器", "incline_press", "HB-3413", "gy_ble25t2"));
        equipment.add(demoEquipment(3L, "EQ-000003", "伸屈腿训练器", "leg_extension_curl", "HB-3414", "gy_ble25t3"));
        equipment.add(demoEquipment(4L, "EQ-000004", "蹬腿训练器", "leg_press", "HB-3415", "gy_ble25t4"));
        return equipment;
    }

    private Map<String, Object> demoEquipment(Long id, String code, String name, String type, String deviceCode, String bluetoothName) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("equipmentId", id);
        item.put("equipmentCode", code);
        item.put("equipmentName", name);
        item.put("equipmentType", type);
        item.put("equipmentCategory", "strength");
        item.put("deviceId", id);
        item.put("deviceCode", deviceCode);
        item.put("bluetoothName", bluetoothName);
        return item;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> applyVenueEquipmentTasks(Map<String, Object> prescription, List<Map<String, Object>> equipment, int age) {
        Map<String, Object> result = new LinkedHashMap<>(prescription);
        List<Map<String, Object>> candidates = equipment.stream()
                .filter(item -> "strength".equals(item.get("equipmentCategory")))
                .limit(age >= 60 ? 3 : 4)
                .toList();
        if (candidates.isEmpty()) {
            candidates = equipment.stream().limit(3).toList();
        }

        List<Map<String, Object>> tasks = new ArrayList<>();
        int idx = 1;
        for (Map<String, Object> item : candidates) {
            String type = stringValue(item.get("equipmentType"), "strength");
            Map<String, Object> task = new LinkedHashMap<>();
            task.put("taskId", idx);
            task.put("exerciseName", item.get("equipmentName"));
            task.put("exerciseType", type);
            task.put("equipmentCode", item.get("equipmentCode"));
            task.put("equipmentName", item.get("equipmentName"));
            task.put("equipmentType", type);
            task.put("equipmentCategory", item.get("equipmentCategory"));
            task.put("targetSets", idx == 1 && age >= 60 ? 2 : 3);
            task.put("targetReps", age >= 60 ? 10 : 12);
            task.put("targetLoadKg", defaultLoadForEquipment(type));
            task.put("targetHr", null);
            task.put("intensityLabel", age >= 60 ? "低中强度" : "中等强度");
            task.put("restSeconds", age >= 60 ? 90 : 75);
            task.put("status", "pending");
            task.put("coachingTip", "扫码器械二维码后开始，保持动作轨迹稳定。");
            tasks.add(task);
            idx++;
        }

        int plannedSets = tasks.stream().mapToInt(task -> ((Number) task.get("targetSets")).intValue()).sum();
        result.put("tasks", tasks);
        result.put("plannedSessions", plannedSets);
        result.put("totalSets", plannedSets);
        result.put("aiSuggestion", "已根据当前场馆可用器械生成今日力量康复训练，按任务顺序扫码对应器械开始。");
        result.put("coachingReasoning", "根据用户阶段、今日完成情况和当前场馆可用器械自动挑选。");
        result.put("exerciseGoal", "今日场馆器械训练");
        result.put("exerciseGoalEn", "venue_equipment_plan");
        result.put("venueEquipmentCount", equipment.size());
        return result;
    }

    private String equipmentCategory(String equipmentType) {
        String type = stringValue(equipmentType, "strength");
        if (Set.of("treadmill", "rowing", "cycling").contains(type)) {
            return "cardio";
        }
        if (Set.of("body_fat_scale", "body_composition", "scale").contains(type)) {
            return "body_composition";
        }
        return "strength";
    }

    private double defaultLoadForEquipment(String equipmentType) {
        return switch (stringValue(equipmentType, "strength")) {
            case "leg_press", "squat" -> 30.0;
            case "leg_extension_curl", "shoulder_shrug", "biceps" -> 15.0;
            case "treadmill", "body_fat_scale", "body_composition" -> 0.0;
            default -> 20.0;
        };
    }

    private Map<String, Object> deviceExerciseTask(int taskId, String exerciseName, String exerciseType,
            int targetSets, int targetReps, double targetLoadKg, int restSeconds,
            String intensityLabel, String coachingTip) {
        Map<String, Object> task = new LinkedHashMap<>();
        task.put("taskId", taskId);
        task.put("exerciseName", exerciseName);
        task.put("exerciseType", exerciseType);
        task.put("targetSets", targetSets);
        task.put("targetReps", targetReps);
        task.put("targetLoadKg", targetLoadKg);
        task.put("restSeconds", restSeconds);
        task.put("intensityLabel", intensityLabel);
        task.put("coachingTip", coachingTip);
        task.put("status", "pending");
        return task;
    }

    private String cardioExerciseName(String deviceType) {
        return switch (stringValue(deviceType, "treadmill")) {
            case "rowing", "rowing_machine" -> "划船训练";
            case "cycling", "spin_bike" -> "骑行训练";
            default -> "跑步训练";
        };
    }

    private List<String> muscleGroupsForDevice(String deviceType) {
        return switch (stringValue(deviceType, "strength_station")) {
            case "chest_press", "incline_press" -> List.of("胸部", "肩部", "肱三头肌");
            case "leg_press", "leg_extension_curl", "squat" -> List.of("股四头肌", "臀部", "腘绳肌");
            case "rowing", "rowing_machine" -> List.of("背部", "核心", "腿部");
            case "treadmill", "cycling", "spin_bike" -> List.of("心肺", "腿部");
            case "body_fat_scale", "body_composition", "scale" -> List.of("身体成分");
            default -> List.of("全身力量", "核心稳定");
        };
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // Fallback Data (B Line本地规则)
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * 构建本地规则处方
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> buildFallbackPrescription(String userId, String deviceType, int age) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("date", LocalDate.now().toString());
        result.put("plannedSessions", 4);
        result.put("completedSessions", 0);
        result.put("totalDurationMin", 1);
        result.put("totalSets", 1);
        result.put("totalReps", 0);
        result.put("complianceRate", 0);

        List<Map<String, Object>> tasks = new ArrayList<>();

        Map<String, Object> task1 = new LinkedHashMap<>();
        task1.put("taskId", 1);
        task1.put("exerciseName", "慢跑热身");
        task1.put("exerciseType", "warmup");
        task1.put("targetSets", 1);
        task1.put("targetReps", 5);
        task1.put("targetLoadKg", 0);
        task1.put("targetHr", null);
        task1.put("intensityLabel", "低强度");
        task1.put("restSeconds", 60);
        task1.put("status", "pending");
        task1.put("coachingTip", "热身很重要，建议先慢走2分钟再开始");
        tasks.add(task1);

        String mainExercise = getExerciseNameByDevice(deviceType);
        Map<String, Object> task2 = new LinkedHashMap<>();
        task2.put("taskId", 2);
        task2.put("exerciseName", mainExercise);
        task2.put("exerciseType", getExerciseType(deviceType));
        task2.put("targetSets", 3);
        task2.put("targetReps", 12);
        task2.put("targetLoadKg", getDefaultLoad(deviceType));
        task2.put("targetHr", null);
        task2.put("intensityLabel", "中等强度");
        task2.put("restSeconds", 90);
        task2.put("status", "pending");
        task2.put("coachingTip", "量力而行，注意保持正确姿势");
        tasks.add(task2);

        result.put("tasks", tasks);
        result.put("aiSuggestion", "干预引擎暂时不可用，请稍后刷新获取个性化方案。");
        result.put("coachingReasoning", "系统处于降级模式，连接干预引擎失败。");
        result.put("exerciseGoal", "综合体质提升");
        result.put("exerciseGoalEn", "general_fitness");
        result.put("userStage", getStageByAge(age));
        result.put("intensityZone", "中等强度");
        result.put("targetHrZone", null);

        List<Map<String, Object>> tips = new ArrayList<>();
        Map<String, Object> tip1 = new LinkedHashMap<>();
        tip1.put("title", "运动前热身");
        tip1.put("content", "每次运动前做5-10分钟热身，减少受伤风险");
        tip1.put("evidenceSource", "ACSM 2021");
        tip1.put("category", "training");
        tips.add(tip1);
        result.put("healthTips", tips);

        result.put("algorithmVersion", "XIN-AE-fallback");
        result.put("fallback", true);
        return result;
    }

    private String getExerciseNameByDevice(String deviceType) {
        if (StringUtils.isEmpty(deviceType)) return "综合训练";
        return switch (deviceType) {
            case "跑步机", "treadmill" -> "跑步训练";
            case "力量站", "strength_station" -> "力量训练";
            case "划船机", "rowing_machine" -> "划船训练";
            case "椭圆机", "elliptical" -> "椭圆机训练";
            case "动感单车", "spin_bike" -> "骑行训练";
            default -> "综合训练";
        };
    }

    private String getExerciseType(String deviceType) {
        if (StringUtils.isEmpty(deviceType)) return "mixed";
        return switch (deviceType) {
            case "跑步机", "treadmill" -> "treadmill";
            case "力量站", "strength_station" -> "strength";
            case "划船机", "rowing_machine" -> "rowing";
            case "椭圆机", "elliptical" -> "elliptical";
            case "动感单车", "spin_bike" -> "cycling";
            default -> "mixed";
        };
    }

    private double getDefaultLoad(String deviceType) {
        if (StringUtils.isEmpty(deviceType)) return 0.0;
        return switch (deviceType) {
            case "力量站", "strength_station" -> 20.0;
            case "划船机", "rowing_machine" -> 30.0;
            case "椭圆机", "elliptical" -> 0.0;
            case "动感单车", "spin_bike" -> 3.0;
            default -> 0.0;
        };
    }

    private String getStageByAge(int age) {
        if (age < 25) return "beginner";
        if (age < 40) return "growth";
        if (age < 55) return "stable";
        return "maintenance";
    }
}
