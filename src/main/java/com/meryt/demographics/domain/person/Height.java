package com.meryt.demographics.domain.person;

import lombok.NonNull;

class Height {

    // This is the average height of an adult male.  The difference between a
    // given person's adult height and this height will be applied
    // proportionally to the 50th percentile heights at each age to calculate
    // his heights at various ages as a child.
	private static double AVG_ADULT_MALE_HEIGHT = 69.6850394;  // abt. 5'10" (177cm)
	private static double AVG_ADULT_FEMALE_HEIGHT = 64.3700787;  // abt. 5'4.3" (163.5cm)

    // The following two arrays contain the heights in centimeters at age 0
    // through 21.
    // Note that the heights for ages 0 and 1 are 0 because we are using a
    // different, month-by-month calculation for babies and toddlers aged 0 to 2.

    // The following values are for the 50th percentile at
    // http://www.keepkidshealthy.com/growthcharts/boystwoyears.gif
    // which gives a default avg. adult height of 177 cm (5'9.5")
    private static double[] BOY_HEIGHTS_CM = {0, 0, 88, 95, 102, 109, 115.5,
            122, 127.5, 133.5, 138.5, 143.5, 149, 156, 164, 170, 173.5, 175, 176,
            176.5, 177};
    private static double[] GIRL_HEIGHTS_CM = {0, 0, 86.1, 93.5, 100.5, 107.5, 115,
            121.5, 127.5, 133, 138.5, 143.5, 151, 157.5, 160.5, 162, 162.5, 163,
            163, 163.5, 163.5};

    // Babies' heights are indexed by MONTH not YEAR.
    // These two arrays are entered in INCHES not CENTIMETERS!!
    private static double[] BABY_BOY_HEIGHTS_IN = {21.5,23,24.25,25.25,26,26.5,27.2,27.6,28.3,
            28.7,29.4,29.8,30.3,30.7,31.1,31.5,32,32.4,32.8,33,33.4,33.7,34.1,34.3,
            34.6};
    private static double[] BABY_GIRL_HEIGHTS_IN = {21,22.25,23.5,24.5,25,25.1,25.7,26.5,
            27.2,27.6,28.1,28.6,29.1,29.6,30,30.4,30.9,31.2,31.7,32.1,32.5,32.7,
            33.1,33.5,33.8,33.9};

    private Height() {
        // hide constructor
    }

    static Double calculateHeight(final double heightInches, long ageInDays, @NonNull Gender gender) {
        if (ageInDays < 0) {
            return null;
        }

        double ageInYears = Math.floor(ageInDays / 365);
        if (20 <= ageInYears) {
            return heightInches;
        }

        // There may be a small window of time where is 24 months old but not yet 2 years (because we divide by 30
        // rather than calculating actual age). In this case, treat him as 2.
        double ageInMonths = Math.floor(ageInDays / 30);
        if (24 == ageInMonths) {
            ageInYears = 2;
            ageInDays = 365 * 2;
        }

        boolean isMale = gender == Gender.MALE;

        double height = heightInches;

        // This will be the multiplicative factor used on the average height as calculated for this age.
        double factor = height / (isMale ? AVG_ADULT_MALE_HEIGHT : AVG_ADULT_FEMALE_HEIGHT);

        double height1, height2;
        double[] heightArr;
        int index;
        // Tells how many days fit into each "slot" in the appropriate height array. Baby heights are per month and
        // child heights are per year.
        int daysPerSlot;
        double cmToInchesConversionFactor = 1;
        if (ageInYears >= 2) {
            heightArr = isMale ? BOY_HEIGHTS_CM : GIRL_HEIGHTS_CM;
            index = (int) ageInYears;
            daysPerSlot = 365;
            cmToInchesConversionFactor = 0.393700787;
        } else {
            heightArr = isMale ? BABY_BOY_HEIGHTS_IN : BABY_GIRL_HEIGHTS_IN;
            index = (int) ageInMonths;
            daysPerSlot = 30;
        }

        height1 = heightArr[index];
        height2 = heightArr[index + 1];

        // Do a linear interpolation of the average height based on the value in their current month's or year's slot,
        // and the value in the next month's or year's slot, based on their current age in days.
        height = height1 + ((height2 - height1) * (ageInDays % daysPerSlot) / daysPerSlot);
        // Convert cm to in if necessary (baby heights are in cm)
        height *= cmToInchesConversionFactor;
        // Apply a factor based on the deviation of their adult height from the average height of their gender.
        height *= factor;

        return height;
    }

}
