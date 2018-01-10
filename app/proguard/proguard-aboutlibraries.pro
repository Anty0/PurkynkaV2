#-keepclasseswithmembers class **.R$* {
#    public static final int define_*;
#}

-keep class .R
-keep class **.R$* {
    <fields>;
}
