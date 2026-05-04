package com.ruoyi.intervention.domain.enums;

/**
 * Dimensions of an intervention plan that can be adjusted.
 *
 * Reference: ACSM 2021 Ch.4 progressive overload; Ch.7 exercise prescription
 */
public enum AdjustmentDimension {
    /** Exercise intensity (RPE, HR zone, speed) */
    INTENSITY,
    /** Session duration in minutes */
    DURATION,
    /** Sessions per week frequency */
    FREQUENCY,
    /** Exercise type (aerobic vs strength vs flexibility) */
    TYPE,
    /** Rest days between sessions */
    RECOVERY,
    /** Sleep intervention parameters */
    SLEEP_HYGIENE,
    /** Nutritional prescription parameters */
    NUTRITION
}
