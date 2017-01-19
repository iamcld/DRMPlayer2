package com.iwanghang.drmplayer.custom;

import android.content.Context;
import android.widget.TextView;

/**
 * Created by iwanghang on 16/5/12.
 */
public class PlayView extends TextView {
    private String TtileText = "歌名";

    public PlayView(Context context) {
        super(context);
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        super.setText(text, type);
    }

    public String getTtileText() {
        return TtileText;
    }

    public void setTtileText(String text){
        TtileText = text;
    }


}
