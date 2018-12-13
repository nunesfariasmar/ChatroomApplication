package mcc.mcc18.Utils;

import java.util.ArrayList;
import java.util.Arrays;

public class MLOptions {
    private static final String categories = "food,technology,screenshot";
    public static final String defaultLabel = "others";
    public static final ArrayList<String> availableLabels = new ArrayList<String>(Arrays.asList(categories.split(",")));
}
