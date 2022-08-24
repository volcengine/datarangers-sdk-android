// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.exposure;

import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.StateSet;

public class DrawableWrapper extends Drawable implements Drawable.Callback {
    private Drawable mDrawable;

    public DrawableWrapper(Drawable drawable) {
        this.setWrappedDrawable(drawable);
    }

    public void draw(Canvas canvas) {
        if (this.mDrawable != null) {
            this.mDrawable.draw(canvas);
        }
    }

    protected void onBoundsChange(Rect bounds) {
        if (this.mDrawable != null) {
            this.mDrawable.setBounds(bounds);
        }
    }

    public void setChangingConfigurations(int configs) {
        if (this.mDrawable != null) {
            this.mDrawable.setChangingConfigurations(configs);
        }
    }

    public int getChangingConfigurations() {
        if (this.mDrawable != null) {
            return this.mDrawable.getChangingConfigurations();
        }
        return 0;
    }

    public void setDither(boolean dither) {
        if (this.mDrawable != null) {
            this.mDrawable.setDither(dither);
        }
    }

    public void setFilterBitmap(boolean filter) {
        if (this.mDrawable != null) {
            this.mDrawable.setFilterBitmap(filter);
        }
    }

    public void setAlpha(int alpha) {
        if (this.mDrawable != null) {
            this.mDrawable.setAlpha(alpha);
        }
    }

    public void setColorFilter(ColorFilter cf) {
        if (this.mDrawable != null) {
            this.mDrawable.setColorFilter(cf);
        }
    }

    public boolean isStateful() {
        if (this.mDrawable != null) {
            return this.mDrawable.isStateful();
        }
        return false;
    }

    public boolean setState(int[] stateSet) {
        if (this.mDrawable != null) {
            return this.mDrawable.setState(stateSet);
        }
        return false;
    }

    public int[] getState() {
        if (this.mDrawable != null) {
            return this.mDrawable.getState();
        }
        return StateSet.WILD_CARD;
    }

    public void jumpToCurrentState() {
        if (this.mDrawable != null) {
            this.mDrawable.jumpToCurrentState();
        }
    }

    public Drawable getCurrent() {
        if (this.mDrawable != null) {
            return this.mDrawable.getCurrent();
        }
        return this;
    }

    public boolean setVisible(boolean visible, boolean restart) {
        if (this.mDrawable != null) {
            return this.mDrawable.setVisible(visible, restart);
        }
        return false;
    }

    public int getOpacity() {
        if (this.mDrawable != null) {
            return this.mDrawable.getOpacity();
        }
        return PixelFormat.UNKNOWN;
    }

    public Region getTransparentRegion() {
        if (this.mDrawable != null) {
            return this.mDrawable.getTransparentRegion();
        }
        return null;
    }

    public int getIntrinsicWidth() {
        if (this.mDrawable != null) {
            return this.mDrawable.getIntrinsicWidth();
        }
        return super.getIntrinsicWidth();
    }

    public int getIntrinsicHeight() {
        if (this.mDrawable != null) {
            return this.mDrawable.getIntrinsicHeight();
        }
        return super.getIntrinsicHeight();
    }

    public int getMinimumWidth() {
        if (this.mDrawable != null) {
            return this.mDrawable.getMinimumWidth();
        }
        return super.getMinimumWidth();
    }

    public int getMinimumHeight() {
        if (this.mDrawable != null) {
            return this.mDrawable.getMinimumHeight();
        }
        return super.getMinimumHeight();
    }

    public boolean getPadding(Rect padding) {
        if (this.mDrawable != null) {
            return this.mDrawable.getPadding(padding);
        }
        return false;
    }

    public void invalidateDrawable(Drawable who) {
        this.invalidateSelf();
    }

    public void scheduleDrawable(Drawable who, Runnable what, long when) {
        this.scheduleSelf(what, when);
    }

    public void unscheduleDrawable(Drawable who, Runnable what) {
        this.unscheduleSelf(what);
    }

    protected boolean onLevelChange(int level) {
        if (this.mDrawable != null) {
            return this.mDrawable.setLevel(level);
        }
        return false;
    }

    public void setAutoMirrored(boolean mirrored) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (this.mDrawable != null) {
                this.mDrawable.setAutoMirrored(mirrored);
            }
        }
    }

    public boolean isAutoMirrored() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (this.mDrawable != null) {
                return this.mDrawable.isAutoMirrored();
            }
        }
        return false;
    }

    public void setTint(int tint) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (this.mDrawable != null) {
                mDrawable.setTint(tint);
            }
        }
    }

    public void setTintList(ColorStateList tint) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (this.mDrawable != null) {
                mDrawable.setTintList(tint);
            }
        }
    }

    public void setTintMode(PorterDuff.Mode tintMode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (this.mDrawable != null) {
                mDrawable.setTintMode(tintMode);
            }
        }
    }

    public void setHotspot(float x, float y) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (this.mDrawable != null) {
                mDrawable.setHotspot(x, y);
            }
        }
    }

    public void setHotspotBounds(int left, int top, int right, int bottom) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (this.mDrawable != null) {
                mDrawable.setHotspotBounds(left, top, right, bottom);
            }
        }
    }

    public Drawable getWrappedDrawable() {
        return this.mDrawable;
    }

    public void setWrappedDrawable(Drawable drawable) {
        if (this.mDrawable != null) {
            if (this.mDrawable != null) {
                this.mDrawable.setCallback((Callback) null);
            }
        }

        this.mDrawable = drawable;
        if (drawable != null) {
            drawable.setCallback(this);
        }

    }
}
