package com.jerry.micbridge;

/** Original Studio palettes shared by native screens and platform-independent contrast tests. */
public final class ThemePalette {
    public static final String[] NAMES={"Follow system","Studio Light","Graphite Dark","Midnight OLED"};
    public final boolean dark;
    public final int background,surface,ink,secondary,accent,soft,border,warning;
    private ThemePalette(boolean dark,int bg,int paper,int ink,int sub,int accent,int soft,int border,int warning){this.dark=dark;background=bg;surface=paper;this.ink=ink;secondary=sub;this.accent=accent;this.soft=soft;this.border=border;this.warning=warning;}
    public static ThemePalette resolve(int mode,boolean systemDark){
        if(mode==3)return new ThemePalette(true,0xFF000000,0xFF101416,0xFFF2F7F4,0xFFA8B8B1,0xFF83DEBF,0xFF17382C,0xFF29372F,0xFF452923);
        if(mode==2||(mode==0&&systemDark))return new ThemePalette(true,0xFF101218,0xFF1B202A,0xFFF2F4FA,0xFFAFB8C8,0xFF8BDFD0,0xFF233D3A,0xFF323A47,0xFF452923);
        return new ThemePalette(false,0xFFF3F4F7,0xFFFFFFFF,0xFF191F2A,0xFF5B6575,0xFF007363,0xFFE1F3ED,0xFFE2E6ED,0xFFFFE8E3);
    }
    public int buttonText(int fill){return fill==ink?background:fill==accent?(dark?background:0xFFFFFFFF):ink;}
}
