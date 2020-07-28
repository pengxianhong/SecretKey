package com.pengxh.secretkey.widgets;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.pengxh.app.multilib.utils.SaveKeyValues;
import com.pengxh.app.multilib.widget.EasyToast;
import com.pengxh.secretkey.R;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author: Pengxh
 * @email: 290677893@qq.com
 * @description: TODO
 * @date: 2020/7/28 16:32
 */
public class GestureLockView extends View {
    //手势初始化录入状态
    public static final int STATE_REGISTER = 101;
    //手势确认 使用状态
    public static final int STATE_LOGIN = 100;
    //设置一个参数记录当前是出于初始化阶段还是使用阶段，默认为确认状态
    private int stateFlag = STATE_LOGIN;
    //获取上下
    private Context mContext;
    //定义计时器
    private Timer mTimer;
    private TimerTask mTimerTask;
    //记录是否失败次数超过限制
    private boolean mTimeOut;
    //剩余的等待时间
    private int leftTime;
    private int waitTime;
    //记录是否手势密码处于可用状态
    private boolean mError;
    //尝试失败的最大次数 默认为5
    private int tempCount;
    //最小设置手势的点数
    private int minPointNumbers;
    //定义一个接口
    private GestureCallBack gestureCallBack;
    //定义两个存储的list
    private List<GestureBean> gestureData;
    private List<GestureBean> gestureDataCopy;

    //用于储存最后一个点的坐标
    private GestureBean lastGesture = null;

    private Bitmap selectedBitmap;
    private Bitmap unSelectedBitmap;
    private Bitmap selectedBitmapSmall;
    private Bitmap unSelectedBitmapSmall;

    //一行3*1单位行高
    private float mLineHeight;
    //给小手势view留的空间
    private static int panelHeight = 300;
    //view经过measure之后的宽度
    private int mPanelWidth;
    //单元控件的宽度
    private float pieceWidth;
    private float pieceWidthSmall;

    private String message = "请绘制手势";
    private float currX;
    private float currY;

    private Paint mPaint;

    public GestureLockView(Context context) {
        this(context, null);
    }

    public GestureLockView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GestureLockView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.GestureLockView);
        Drawable selected = typedArray.getDrawable(R.styleable.GestureLockView_selectedBitmap);
        Drawable unSelect = typedArray.getDrawable(R.styleable.GestureLockView_unselectedBitmap);
        Drawable selectedSmall = typedArray.getDrawable(R.styleable.GestureLockView_selectedBitmapSmall);
        Drawable unSelectSmall = typedArray.getDrawable(R.styleable.GestureLockView_unselectedBitmapSmall);
        if (selected != null) {
            selectedBitmap = ((BitmapDrawable) selected).getBitmap();
        } else {
            selectedBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.finger_selected);
        }
        if (unSelect != null) {
            unSelectedBitmap = ((BitmapDrawable) unSelect).getBitmap();
        } else {
            unSelectedBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.finger_unselected);
        }
        if (selectedSmall != null) {
            selectedBitmapSmall = ((BitmapDrawable) selectedSmall).getBitmap();
        } else {
            selectedBitmapSmall = BitmapFactory.decodeResource(getResources(), R.mipmap.finger_selected_small);
        }
        if (unSelectSmall != null) {
            unSelectedBitmapSmall = ((BitmapDrawable) unSelectSmall).getBitmap();
        } else {
            unSelectedBitmapSmall = BitmapFactory.decodeResource(getResources(), R.mipmap.finger_unselected_small);
        }
        //等待时间,默认30s
        waitTime = typedArray.getInteger(R.styleable.GestureLockView_waitTime, 30);
        //尝试次数，默认5
        tempCount = typedArray.getInteger(R.styleable.GestureLockView_maxFailCounts, 5);
        //最小设置的点,默认4个
        minPointNumbers = typedArray.getInteger(R.styleable.GestureLockView_minPoint, 4);
        //设置画笔的颜色
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setStrokeWidth(10);
        mPaint.setStyle(Paint.Style.STROKE);
        //画笔的颜色
        int color = typedArray.getColor(R.styleable.GestureLockView_paintColor, Color.BLACK);
        mPaint.setColor(color);
        //字体的大小
        float textSize = typedArray.getDimension(R.styleable.GestureLockView_paintTextSize, 40);
        mPaint.setTextSize(textSize);
        //避免重新创建时候的错误
        typedArray.recycle();

        initView(context);
    }

    private void initView(Context mContext) {
        this.mContext = mContext;
        //让当前的Activity继承View的接口
        try {
            gestureCallBack = (GestureCallBack) mContext;
        } catch (final ClassCastException e) {
            throw new ClassCastException(mContext.toString() + " must implement GestureCallBack");
        }
        mTimer = new Timer();
        //计算上次失败时间与现在的时间差
        try {
            long lastTime = (long) SaveKeyValues.getValue("gestureTime", 0L);
            Date date = new Date();
            if (lastTime != 0 && (date.getTime() - lastTime) / 1000 < waitTime) {
                //失败时间未到，还处于锁定状态
                mTimeOut = true;
                leftTime = (int) (waitTime - ((date.getTime() - lastTime)) / 1000);
                mTimerTask = new InnerTimerTask(handler);
                mTimer.schedule(mTimerTask, 0, 1000);
            } else {
                mTimeOut = false;
                leftTime = waitTime;
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        gestureData = new ArrayList<>();
        gestureDataCopy = new ArrayList<>();
        stateFlag = getState();

    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(@NotNull Message msg) {
            leftTime--;
            if (leftTime == 0) {
                if (mTimer != null)
                    mTimerTask.cancel();
                mTimeOut = false;
                EasyToast.showToast("请绘制解锁图案", EasyToast.DEFAULT);
                mError = false;
                invalidate();
                //将计时信息还原
                reSet();
                return;
            }
            mError = true;
            invalidate();
        }
    };

    private void reSet() {
        leftTime = waitTime;
        tempCount = 5;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        //width即为大View的单位宽 高
        int width = Math.min(widthSize, heightSize);
        if (widthMode == MeasureSpec.UNSPECIFIED) {
            width = heightSize;
        } else if (heightMode == MeasureSpec.UNSPECIFIED) {
            width = widthSize;
        }
        //大View一行3*1单位行高
        mLineHeight = width / 3;
        //大手势View为边长width的正方形,panelHeight是给小手势view预留的空间
        setMeasuredDimension(width, width + panelHeight);
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mPanelWidth = Math.min(w, h);
        //大手势点宽度，为单位宽高的0.6倍，显得更好看一些不会很满
        pieceWidth = (int) (mLineHeight * 0.6f);
        //小手势点宽度，同理
        pieceWidthSmall = (int) (mLineHeight * 0.15f);
        //画出对应手势点的大小
        selectedBitmap = Bitmap.createScaledBitmap(selectedBitmap, (int) pieceWidth, (int) pieceWidth, false);
        unSelectedBitmap = Bitmap.createScaledBitmap(unSelectedBitmap, (int) pieceWidth, (int) pieceWidth, false);
        selectedBitmapSmall = Bitmap.createScaledBitmap(selectedBitmapSmall, (int) pieceWidthSmall, (int) pieceWidthSmall, false);
        unSelectedBitmapSmall = Bitmap.createScaledBitmap(unSelectedBitmapSmall, (int) pieceWidthSmall, (int) pieceWidthSmall, false);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //如果处于初始化状态
        if (stateFlag == STATE_REGISTER) {
            //绘制上面的提示点  不需要提示点
            drawTipsPoint(canvas);
        } else {
            //上面的是文字 点没了
            drawTipsText(canvas);
        }

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                canvas.drawBitmap(unSelectedBitmap, (float) (mLineHeight * (j + 0.5) - pieceWidth / 2), (float) (mLineHeight * (i + 0.5) - pieceWidth / 2 + panelHeight), mPaint);
            }
        }
        //用于判断状态
        GestureBean firstGesture;
        GestureBean currGesture;
        if (!gestureData.isEmpty()) {
            firstGesture = gestureData.get(0);
            //画连接线
            for (int i = 1; i < gestureData.size(); i++) {
                currGesture = gestureData.get(i);
                canvas.drawLine((float) (mLineHeight * (firstGesture.getX() + 0.5)), (float) (mLineHeight * (firstGesture.getY() + 0.5) + panelHeight), (float) (mLineHeight * (currGesture.getX() + 0.5)), (float) (mLineHeight * (currGesture.getY() + 0.5) + panelHeight), mPaint);
                firstGesture = currGesture;
            }
            //最后一条线
            lastGesture = gestureData.get(gestureData.size() - 1);
            canvas.drawLine((float) (mLineHeight * (lastGesture.getX() + 0.5)), (float) (mLineHeight * (lastGesture.getY() + 0.5) + panelHeight), currX, currY, mPaint);

            //遍历数组，把把选中的点更换图片
            for (GestureBean bean : gestureData) {
                canvas.drawBitmap(selectedBitmap, (float) (mLineHeight * (bean.getX() + 0.5) - pieceWidth / 2), (float) (mLineHeight * (bean.getY() + 0.5) + panelHeight - pieceWidth / 2), mPaint);
            }
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //失败情况 不允许操作
        if (mTimeOut) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    break;
                case MotionEvent.ACTION_UP:
                    if (0 < leftTime && leftTime <= 30) {
                        EasyToast.showToast("尝试次数达到最大," + leftTime + "s后重试", EasyToast.WARING);
                    }
                    return true;
            }
        }
        //判断手势点在大View内
        if (event.getY() >= ((mLineHeight * (0 + 0.5) - pieceWidth / 2 + panelHeight))) {
            //得到XY用于判断 手指处于哪个点
            int x = (int) ((event.getY() - panelHeight) / mLineHeight);
            int y = (int) (event.getX() / mLineHeight);

            //当前手指的坐标
            currX = event.getX();
            currY = event.getY();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastGesture = null;
                    if (currX >= 0 && currX <= mPanelWidth && currY >= panelHeight && currY <= panelHeight + mPanelWidth) {
                        if (currY <= (x + 0.5) * mLineHeight + pieceWidth / 2 + panelHeight && currY >= (x + 0.5) * mLineHeight - pieceWidth / 2 + panelHeight &&
                                currX <= (y + 0.5) * mLineHeight + pieceWidth / 2 && currX >= (y + 0.5) * mLineHeight - pieceWidth / 2) {
                            //判断当前手指处于哪个点范围内，如果点没存在listData,存进去，第一个点
                            if (!gestureData.contains(new GestureBean(y, x))) {
                                gestureData.add(new GestureBean(y, x));
                            }
                        }
                    }
                    //重绘一次，第一个点显示被选中了
                    invalidate();
                    break;
                case MotionEvent.ACTION_MOVE:
                    //手指移动在大View范围内
                    if (currX >= 0 && currX <= mPanelWidth && currY >= panelHeight && currY <= panelHeight + mPanelWidth) {
                        //缩小响应范围 在此处需要注意的是 x跟currX在物理方向上是反的哦
                        if (currY <= (x + 0.5) * mLineHeight + pieceWidth / 2 + panelHeight && currY >= (x + 0.5) * mLineHeight - pieceWidth / 2 + panelHeight &&
                                currX <= (y + 0.5) * mLineHeight + pieceWidth / 2 && currX >= (y + 0.5) * mLineHeight - pieceWidth / 2) {
                            //滑倒的店处于哪个点范围内，如果点没存在listData,存进去
                            if (!gestureData.contains(new GestureBean(y, x))) {
                                gestureData.add(new GestureBean(y, x));
                            }
                        }
                    }
                    //重绘
                    invalidate();
                    break;
                case MotionEvent.ACTION_UP:
                    if (lastGesture != null) {
                        currX = (float) ((lastGesture.getX() + 0.5) * mLineHeight);
                        currY = (float) ((lastGesture.getY() + 0.5) * mLineHeight);
                    }
                    //如果View处于认证状态
                    if (stateFlag == STATE_LOGIN) {
                        //相同那么认证成功
                        if (gestureData.equals(loadGestureData())) {
                            mError = false;
                            postListener(true);
                            invalidate();
                            gestureData.clear();
                            return true;
                        } else {
                            if (--tempCount == 0) {//尝试次数达到上限
                                mError = true;
                                mTimeOut = true;
                                gestureData.clear();
                                Date date = new Date();
                                SaveKeyValues.putValue("gestureTime", date.getTime());
                                mTimerTask = new InnerTimerTask(handler);
                                mTimer.schedule(mTimerTask, 0, 1000);
                                invalidate();
                                return true;
                            }
                            mError = true;
                            EasyToast.showToast("手势错误,还可以再输入" + tempCount + "次", EasyToast.ERROR);
                            gestureData.clear();
                        }

                    }
                    //View处于注册状态
                    else if (stateFlag == STATE_REGISTER) {
                        //第一次认证状态
                        if (gestureDataCopy == null || gestureDataCopy.isEmpty()) {
                            if (gestureData.size() < minPointNumbers) {
                                gestureData.clear();
                                mError = true;
                                EasyToast.showToast("点数不能小于" + minPointNumbers + "个", EasyToast.WARING);
                                invalidate();
                                return true;
                            }
                            gestureDataCopy.addAll(gestureData);
                            gestureData.clear();
                            mError = false;
                            EasyToast.showToast("请再一次绘制", EasyToast.DEFAULT);
                        } else {
                            //两次认证成功
                            if (gestureData.equals(gestureDataCopy)) {
                                saveGestureData(gestureData);
                                mError = false;
                                stateFlag = STATE_LOGIN;
                                postListener(true);
                                SaveKeyValues.putValue("state", stateFlag);
                            } else {
                                mError = true;
                                EasyToast.showToast("与上次手势绘制不一致，请重新设置", EasyToast.WARING);
                            }
                            gestureData.clear();
                            invalidate();
                            return true;
                        }
                    }
                    invalidate();
                    break;
            }
        }
        return true;
    }

    //读取之前保存的List
    public List<GestureBean> loadGestureData() {
        List<GestureBean> list = new ArrayList<>();
        //取出点数
        int size = (int) SaveKeyValues.getValue("data_size", 0);
        //和坐标
        for (int i = 0; i < size; i++) {
            String str = (String) SaveKeyValues.getValue("data_" + i, "0 0");
            list.add(new GestureBean(Integer.parseInt(str.split(" ")[0]), Integer.parseInt(str.split(" ")[1])));
        }
        return list;
    }

    //将点的xy list存入sp
    private void saveGestureData(List<GestureBean> data) {
        //存入多少个点
        SaveKeyValues.putValue("dataSize", data.size()); /*sKey is an array*/
        //和每个点的坐标
        for (int i = 0; i < data.size(); i++) {
            SaveKeyValues.removeKey("data_" + i);
            SaveKeyValues.putValue("data_" + i, data.get(i).getX() + " " + data.get(i).getY());
        }
    }


    //绘制提示语
    private void drawTipsText(Canvas canvas) {
        float widthMiddleX = mPanelWidth / 2;
        mPaint.setStyle(Paint.Style.FILL);
        int widthStr1 = (int) mPaint.measureText("输入手势来解锁");
        float baseX = widthMiddleX - widthStr1 / 2;
        float baseY = panelHeight / 2 + 50;
        Paint.FontMetrics fontMetrics = mPaint.getFontMetrics();
        float fontTotalHeight = fontMetrics.bottom - fontMetrics.top;
        float offY = fontTotalHeight / 2 - fontMetrics.bottom - 30;
        float newY = baseY + offY;
        canvas.drawText("输入手势来解锁", baseX, newY, mPaint);
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setStrokeWidth(10);
    }


    //绘制提示点
    private void drawTipsPoint(Canvas canvas) {
        //宽度为View宽度的一半
        float widthMiddleX = mPanelWidth / 2;
        //确定好相关坐标，找出第一个点的中心点
        float firstX = widthMiddleX - pieceWidthSmall / 4 - pieceWidthSmall / 2 - pieceWidthSmall;
        float firstY = panelHeight / 2 - pieceWidthSmall / 2 - pieceWidthSmall - pieceWidthSmall / 4 - 10;
        //画点，由于没有选中，画9个未选中点
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                canvas.drawBitmap(unSelectedBitmapSmall, (float) (firstX + j * (pieceWidthSmall * 1.25)), (float) (firstY + i * (pieceWidthSmall * 1.25)), mPaint);
            }
        }
        //第二次确认前的小手势密码·显示第一次划过的痕迹
        if (gestureDataCopy != null && !gestureDataCopy.isEmpty()) {
            for (GestureBean bean : gestureDataCopy) {
                canvas.drawBitmap(selectedBitmapSmall, (float) (firstX + bean.getX() * (pieceWidthSmall * 1.25)), (float) (firstY + bean.getY() * (pieceWidthSmall * 1.25)), mPaint);
            }
        }
        //随着手指ActionMove来改变选中点的颜色
        else if (gestureData != null && !gestureData.isEmpty()) {
            for (GestureBean bean : gestureData) {
                canvas.drawBitmap(selectedBitmapSmall, (float) (firstX + bean.getX() * (pieceWidthSmall * 1.25)), (float) (firstY + bean.getY() * (pieceWidthSmall * 1.25)), mPaint);
            }
        }
        drawMessage(canvas, mError);
    }

    private void drawMessage(Canvas canvas, boolean errorFlag) {
        float widthMiddleX = mPanelWidth / 2;
        //获取Y坐标显示在小View下面
        float firstY = (float) (panelHeight / 2 - pieceWidthSmall / 2 + pieceWidthSmall * 1.25 + 90);

        mPaint.setStrokeWidth(2);
        mPaint.setStyle(Paint.Style.FILL);

        //得到字体的宽度
        int widthStr1 = (int) mPaint.measureText("绘制解锁图案");
        float baseX = widthMiddleX - widthStr1 / 2;
        float baseY = firstY + 40;
        Paint.FontMetrics fontMetrics = mPaint.getFontMetrics();
        float fontTotalHeight = fontMetrics.bottom - fontMetrics.top;
        float offY = fontTotalHeight / 2 - fontMetrics.bottom - 30;
        float newY = baseY + offY;
        canvas.drawText("绘制解锁图案", baseX, newY, mPaint);
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setStrokeWidth(10);
    }


    //从SP中获取当前View处于什么状态，默认为初始化状态
    private int getState() {
        SharedPreferences mSharedPreference = mContext.getSharedPreferences("STATE_DATA", Activity.MODE_PRIVATE);
        return mSharedPreference.getInt("state", STATE_REGISTER);
    }

    //清除以前保存的状态，用于关闭View
    public boolean clearCache() {
        SharedPreferences sp = mContext.getSharedPreferences("STATE_DATA", Activity.MODE_PRIVATE);
        SharedPreferences.Editor edit = sp.edit();
        edit.putInt("state", STATE_REGISTER);
        stateFlag = STATE_REGISTER;
        invalidate();
        return edit.commit();
    }

    //用于更改手势密码，清除以前密码
    public boolean clearCacheLogin() {
        SharedPreferences sp = mContext.getSharedPreferences("STATE_DATA", Activity.MODE_PRIVATE);
        SharedPreferences.Editor edit = sp.edit();
        edit.putInt("state", STATE_LOGIN);
        stateFlag = STATE_LOGIN;
        invalidate();
        return edit.commit();
    }


    //定义一个内部TimerTask类用于记录，错误倒计时
    static class InnerTimerTask extends TimerTask {
        Handler handler;

        InnerTimerTask(Handler handler) {
            this.handler = handler;
        }

        @Override
        public void run() {
            handler.sendMessage(handler.obtainMessage());
        }
    }

    //定义接口 ，传递View状态
    public interface GestureCallBack {
        void onVerifySuccessListener(int stateFlag, List<GestureBean> data, boolean success);
    }

    //给接口传递数据
    private void postListener(boolean success) {
        if (gestureCallBack != null) {
            gestureCallBack.onVerifySuccessListener(stateFlag, gestureData, success);
        }
    }

    //定义Bean，来存储手势坐标
    public static class GestureBean {
        private int x;
        private int y;

        GestureBean(int x, int y) {
            this.x = x;
            this.y = y;
        }

        int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            return ((GestureBean) o).getX() == x && ((GestureBean) o).getY() == y;
        }
    }

    public void setGestureCallBack(GestureCallBack gestureCallBack) {
        this.gestureCallBack = gestureCallBack;
    }


    public int getMinPointNumbers() {
        return minPointNumbers;
    }

    public void setMinPointNumbers(int minPointNumbers) {
        if (minPointNumbers <= 3)
            this.minPointNumbers = 3;
        if (minPointNumbers >= 9)
            this.minPointNumbers = 9;
    }
}
