/*
 * Copyright (C) 2017 Artem Glugovsky
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dewarder.holdinglibrary;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.animation.AccelerateInterpolator;

public class HoldingDrawable extends Drawable {

    private static final float MIN_EXPANDED_RADIUS_MULTIPLIER = 0.3f;
    private static final long DEFAULT_ANIMATION_DURATION_EXPAND = 150L;
    private static final long DEFAULT_ANIMATION_DURATION_COLLAPSE = 150L;
    private static final long DEFAULT_ANIMATION_DURATION_CANCEL = 200L;
    private static final long DEFAULT_ANIMATION_DURATION_ICON = 200L;

    private Paint mPaint;
    private Paint mSecondPaint;

    private int mIconWidth;
    private int mIconHeight;
    private Matrix mIconMatrix = new Matrix();
    private BitmapShader mIconShader;
    private Paint mIconPaint;

    private int mCancelIconWidth;
    private int mCancelIconHeight;
    private Matrix mCancelIconMatrix = new Matrix();
    private BitmapShader mCancelIconShader;
    private Paint mCancelIconPaint;

    private boolean mIsExpanded = false;
    private boolean mIsCancel = false;

    private ValueAnimator mAnimator;
    private ValueAnimator mCancelAnimator;
    private ValueAnimator mIconAnimator;

    private float mRadius = 120f;
    private float mSecondRadius = 20f;
    private float[] mIconScaleFactor = {1f};
    private float[] mExpandedScaleFactor = {0f};

    private int mDefaultColor = Color.parseColor("#3949AB");
    private int mCancelColor = Color.parseColor("#e53935");
    private int mSecondAlpha = 100;

    private HoldingDrawableListener mListener;

    {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(mDefaultColor);

        mSecondPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mSecondPaint.setColor(mDefaultColor);
        mSecondPaint.setAlpha(mSecondAlpha);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        float centerX = canvas.getWidth() / 2f;
        float centerY = canvas.getHeight() / 2f;
        if (mIsExpanded) {
            if (mSecondRadius > 0) {
                canvas.drawCircle(centerX, centerY, mRadius + mSecondRadius, mSecondPaint);
            }

            float currentRadius = mRadius * (MIN_EXPANDED_RADIUS_MULTIPLIER + (1 - MIN_EXPANDED_RADIUS_MULTIPLIER) * mExpandedScaleFactor[0]);
            canvas.drawCircle(centerX, centerY, currentRadius, mPaint);

            if (mIconPaint != null) {
                Paint iconPaint;
                if (mIsCancel && mCancelIconPaint != null) {
                    iconPaint = mCancelIconPaint;
                    invalidateMatrix(mCancelIconMatrix, centerX, centerY, mCancelIconWidth, mCancelIconHeight);
                    mCancelIconShader.setLocalMatrix(mCancelIconMatrix);
                } else {
                    iconPaint = mIconPaint;
                    invalidateMatrix(mIconMatrix, centerX, centerY, mIconWidth, mIconHeight);
                    mIconShader.setLocalMatrix(mIconMatrix);
                }

                canvas.drawRect(
                        centerX - mIconWidth / 2,
                        centerY - mIconHeight / 2,
                        centerX + mIconWidth / 2,
                        centerY + mIconHeight / 2,
                        iconPaint);
            }
        }
    }

    @Override
    public void setAlpha(@IntRange(from = 0, to = 255) int alpha) {
        mPaint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        mPaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }

    @Override
    public int getIntrinsicHeight() {
        return (int) (mRadius * 2 + mSecondRadius * 2);
    }

    @Override
    public int getIntrinsicWidth() {
        return (int) (mRadius * 2 + mSecondRadius * 2);
    }

    public void expand() {
        notifyOnBeforeExpand();
        mIsExpanded = true;
        if (mAnimator != null) {
            mAnimator.cancel();
        }
        mAnimator = createExpandValueAnimator();
        mAnimator.start();
    }

    public void clickExpand() {
        notifyOnBeforeExpand();
        mIsExpanded = true;
        if (mAnimator != null) {
            mAnimator.cancel();
        }
        mAnimator = createClickExpandValueAnimator();
        mAnimator.start();
    }

    public void collapse() {
        notifyOnBeforeCollapse();
        if (mAnimator != null) {
            mAnimator.cancel();
        }
        mAnimator = createCollapseValueAnimator();
        mAnimator.start();
    }

    public void clickCollapse() {
        notifyOnBeforeCollapse();
        if (mAnimator != null) {
            mAnimator.cancel();
        }
        mAnimator = createClickCollapseValueAnimator();
        mAnimator.start();
    }

    public void reset() {
        mIsExpanded = false;
        mIsCancel = false;
        mPaint.setColor(mDefaultColor);
        mSecondPaint.setColor(mDefaultColor);
        mSecondPaint.setAlpha(mSecondAlpha);
    }

    public void setCancel(boolean isCancel) {
        if (this.mIsCancel != isCancel) {
            this.mIsCancel = isCancel;
            if (mCancelAnimator != null) {
                mCancelAnimator.cancel();
            }
            mCancelAnimator = createCancelValueAnimator();
            mCancelAnimator.start();

            if (mIconAnimator != null) {
                mIconAnimator.cancel();
            }
            mIconAnimator = createIconValueAnimator();
            mIconAnimator.start();
        }
    }

    public void setRadius(float radius) {
        mRadius = radius;
        invalidateSelf();
    }

    public float getRadius() {
        return mRadius;
    }

    @ColorInt
    public int getColor() {
        return mDefaultColor;
    }

    public void setColor(@ColorInt int color) {
        mDefaultColor = color;
        if (!mIsCancel) {
            mPaint.setColor(color);
            mSecondPaint.setColor(color);
            mSecondPaint.setAlpha(mSecondAlpha);
        }
        invalidateSelf();
    }

    @ColorInt
    public int getCancelColor() {
        return mCancelColor;
    }

    public void setCancelColor(int color) {
        mCancelColor = color;
        if (mIsCancel) {
            mPaint.setColor(color);
        }
        invalidateSelf();
    }

    public void setIcon(Bitmap bitmap) {
        if (bitmap != null) {
            mIconWidth = bitmap.getWidth();
            mIconHeight = bitmap.getHeight();
            mIconShader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            mIconShader.setLocalMatrix(mIconMatrix);
            mIconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mIconPaint.setShader(mIconShader);
            mIconPaint.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));

            invalidateSelf();
        } else {
            mIconWidth = 0;
            mIconHeight = 0;
            mIconShader = null;
            mIconMatrix = null;
            mIconPaint = null;
        }
    }

    public void setCancelIcon(Bitmap bitmap) {
        if (bitmap != null) {
            mCancelIconWidth = bitmap.getWidth();
            mCancelIconHeight = bitmap.getHeight();
            mCancelIconShader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            mCancelIconShader.setLocalMatrix(mCancelIconMatrix);
            mCancelIconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mCancelIconPaint.setShader(mCancelIconShader);
            mCancelIconPaint.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));

            invalidateSelf();
        } else {
            mCancelIconWidth = 0;
            mCancelIconHeight = 0;
            mCancelIconMatrix = null;
            mCancelIconPaint = null;
        }
    }

    @IntRange(from = 0, to = 255)
    public int getSecondAlpha() {
        return mSecondAlpha;
    }

    public void setSecondAlpha(@IntRange(from = 0, to = 255) int alpha) {
        mSecondAlpha = alpha;
        invalidateSelf();
    }

    public float getSecondRadius() {
        return mSecondRadius;
    }

    public void setSecondRadius(float radius) {
        mSecondRadius = radius;
        invalidateSelf();
    }

    public void setListener(HoldingDrawableListener listener) {
        mListener = listener;
    }

    private void invalidateMatrix(Matrix matrix, float centerX, float centerY, float width, float height) {
        matrix.reset();
        matrix.setScale(mIconScaleFactor[0], mIconScaleFactor[0]);
        float inverseScaleFactor = 1 - mIconScaleFactor[0];
        matrix.postTranslate(
                centerX - width / 2f + width / 2f * inverseScaleFactor,
                centerY - height / 2f + height / 2f * inverseScaleFactor);
    }

    private ValueAnimator createExpandValueAnimator() {
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(DEFAULT_ANIMATION_DURATION_EXPAND);
        animator.setInterpolator(new AccelerateInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mExpandedScaleFactor[0] = (float) valueAnimator.getAnimatedValue();
                invalidateSelf();
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                notifyExpanded();
            }
        });
        return animator;
    }

    private ValueAnimator createClickExpandValueAnimator() {
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(DEFAULT_ANIMATION_DURATION_EXPAND);
        animator.setInterpolator(new AccelerateInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mExpandedScaleFactor[0] = (float) valueAnimator.getAnimatedValue();
                invalidateSelf();
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                notifyClickExpanded();
            }
        });
        return animator;
    }

    private ValueAnimator createCollapseValueAnimator() {
        ValueAnimator animator = ValueAnimator.ofFloat(1f, 0f);
        animator.setDuration(DEFAULT_ANIMATION_DURATION_COLLAPSE);
        animator.setInterpolator(new AccelerateInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mExpandedScaleFactor[0] = (float) valueAnimator.getAnimatedValue();
                invalidateSelf();
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                notifyCollapsed();
            }
        });
        return animator;
    }

    private ValueAnimator createClickCollapseValueAnimator() {
        ValueAnimator animator = ValueAnimator.ofFloat(1f, 0f);
        animator.setDuration(DEFAULT_ANIMATION_DURATION_COLLAPSE);
        animator.setInterpolator(new AccelerateInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mExpandedScaleFactor[0] = (float) valueAnimator.getAnimatedValue();
                invalidateSelf();
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                notifyClickCollapsed();
            }
        });
        return animator;
    }

    private ValueAnimator createCancelValueAnimator() {
        final int from = mIsCancel ? mDefaultColor : mCancelColor;
        final int to = mIsCancel ? mCancelColor : mDefaultColor;
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator a) {
                int color = ColorHelper.blend(from, to, (float) a.getAnimatedValue());
                mPaint.setColor(color);
                mSecondPaint.setColor(color);
                mSecondPaint.setAlpha(mSecondAlpha);
                invalidateSelf();
            }
        });
        animator.setDuration(DEFAULT_ANIMATION_DURATION_CANCEL);
        return animator;
    }

    private ValueAnimator createIconValueAnimator() {
        ValueAnimator animator = ValueAnimator.ofFloat(0.6f, 1f);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mIconScaleFactor[0] = (float) valueAnimator.getAnimatedValue();
            }
        });
        animator.setDuration(DEFAULT_ANIMATION_DURATION_ICON);
        return animator;
    }

    private void notifyOnBeforeExpand() {
        if (mListener != null) {
            mListener.onBeforeExpand();
        }
    }

    private void notifyOnBeforeCollapse() {
        if (mListener != null) {
            mListener.onBeforeCollapse();
        }
    }

    private void notifyCollapsed() {
        if (mListener != null) {
            mListener.onCollapse();
        }
    }

    private void notifyClickCollapsed() {
        if (mListener != null) {
            mListener.onClickCollapse();
        }
    }

    private void notifyExpanded() {
        if (mListener != null) {
            mListener.onExpand();
        }
    }
    private void notifyClickExpanded() {
        if (mListener != null) {
            mListener.onClickExpand();
        }
    }
}
