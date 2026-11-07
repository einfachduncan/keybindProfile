package dev.keybindprofile;
final class Titles {
    private static final String CAPS="\u1d00\u0299\u1d04\u1d05\u1d07\ua730\u0262\u029c\u026a\u1d0a\u1d0b\u029f\u1d0d\u0274\u1d0f\u1d18q\u0280\ua731\u1d1b\u1d1c\u1d20\u1d21x\u028f\u1d22";
    static String of(String text) {StringBuilder out=new StringBuilder();for(char c:text.toLowerCase(java.util.Locale.ROOT).toCharArray())out.append(c>='a'&&c<='z'?CAPS.charAt(c-'a'):c);return out.toString();}
}
