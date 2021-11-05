package cn.com.mma.mobile.tracking.viewability.origin.sniffer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.com.mma.mobile.tracking.api.Constant;
import cn.com.mma.mobile.tracking.util.klog.KLog;
import cn.com.mma.mobile.tracking.viewability.common.ViewHelper;
import cn.com.mma.mobile.tracking.viewability.origin.support.AtlantisUtil;
import cn.com.mma.mobile.tracking.viewability.origin.support.Rectangle;


/**
 * Created by mma on 17/6/16.
 */
public class ViewFrameSlice implements Serializable {

    private static final long serialVersionUID = 1L;

    //Rect(left,top,right,bottom) left,top = 坐标系左上坐标x点,y点 right,bottom =坐标系右下x点,y点
//    private Rect adWindowRect;
//    private Rect screenRect;
//    private Rect adLocalRect;

    //2t 当前监测点时间戳ms
    private long captureTime;
    //2k 广告实际尺寸 width*height
    private String adSize;
    //2d window坐标系可视区域左上角位置(x*y)
    private String visiblePoint;
    //2o window坐标系可视尺寸
    private String visibleSize;

    //2l 透明度 1.0=完全不透明 0.0=完全透明
    private float alpha;
    //2m 是否显示 0=不显示 1=显示
    private int shown;
    //2r 屏幕是否点亮 1=开屏 0 = 熄灭
    private int screenOn;
    //2s 是否前台运行 0=后台或遮挡 1=前台
    private int isForground;
    //2n 覆盖比例 0.00 - 1.00
    private double coverRate;

    // 结合所有参数判断adView是否可视化 0=不可见 1=可见
    private int visibleAbility;
    // 当前广告是否可测量 0为不可见 1为可见 //默认可见
    //private int measureAbility = 0;
    //判断广告是否在Window窗体之内
    private boolean isWindowShowed = false;

    /** 存放与广告视图覆盖的区域的Rect */



    private List<Rectangle> coverAreaList = null;
    Set<String> coverFrameSet = null;


    public ViewFrameSlice(View adView, Context context) {
        try {
            //时间戳
            captureTime = System.currentTimeMillis();

            coverAreaList = new ArrayList<Rectangle>();

            coverFrameSet = new HashSet<String>();

            //AdView尺寸:宽X高
            int width = adView.getWidth();
            int height = adView.getHeight();
            adSize = width + "x" + height;

            //AdView实际在Window上可视区域坐标(view左上角x,y点)
//            Rect visibleRect = new Rect();
//            adView.getGlobalVisibleRect(visibleRect);
            Rect visibleRect = ViewHelper.getViewInWindowRect(adView);

            Point visibleLeftPoint = new Point();
            visibleLeftPoint.x = visibleRect.left;
            visibleLeftPoint.y = visibleRect.top;
            visiblePoint = visibleLeftPoint.x + "x" + visibleLeftPoint.y;

            boolean checkFrameBounds = checkFrameBounds(adView);
            if (!checkFrameBounds) {
                Rect rect = traverseParent(adView, visibleRect);
                if (rect != null) visibleRect = rect;
            }

            //透明度
            if (Build.VERSION.SDK_INT >= 11) {
                alpha = adView.getAlpha();
            }


            //是否显示
            shown = (adView.isShown()) ? 1 : 0;

            //可视尺寸 在当前屏幕范围内,排除不可见区域后,view的宽和高,滑动时实时变动(和WindowFrame相交运算)
            Rect screenRect = ViewHelper.getScreenRect(context);
            Rect overlapRect = new Rect();
            boolean isIntersets = overlapRect.setIntersect(visibleRect, screenRect);

            int visbleWidth = Math.abs(overlapRect.right - overlapRect.left);
            int visbleHeight = Math.abs(overlapRect.bottom - overlapRect.top);
            //int visbleWidth = visibleRect.right - Math.abs(visibleRect.left);
            //int visbleHeight = visibleRect.bottom - Math.abs(visibleRect.top);
            visibleSize = visbleWidth + "x" + visbleHeight;

            // 如果广告视图自身有问题，则不再统计cover_rate和cover_frame，截图照常
            isWindowShowed = isShowing(adView);

            if (!isWindowShowed) {
                coverRate = 1.00;
//                debugE("the adview:" + contentView + "  is`t show,no need for traverseParent");
            } else {
                // 开始遍历父节上的subview
                traverseParent(adView);

                double totalArea = 0.00;

                // 计算被覆盖率
                if (coverAreaList.size() > 0) {
                    AtlantisUtil au = new AtlantisUtil();
                    totalArea = au.calOverlapArea(coverAreaList);
                }
                //计算广告视图自身在Windows窗体中实际展示的面积大小
                Rect viewRect = new Rect();
                adView.getGlobalVisibleRect(viewRect);

                double adArea = adView.getWidth() * adView.getHeight();
                double windowCoverArea = adArea - (viewRect.right - viewRect.left) * (viewRect.bottom - viewRect.top);

                double temp = (totalArea + windowCoverArea) / adArea;
                coverRate = (double) Math.round(temp * 100) / 100;
//                System.out.println("adArea:" + adArea + "  total:" + totalArea + "  coverrate:" + coverRate + "  temp:" + temp);

            }


            //屏幕是否点亮
            screenOn = ViewHelper.isScreenOn(adView) ? 1 : 0;

            //是否前台运行
            isForground = adView.hasWindowFocus() ? 1 : 0;

            Rect selfRect = new Rect();
            adView.getLocalVisibleRect(selfRect);

            coverAreaList = null;
            coverFrameSet = null;

            KLog.i("=================ViewFrameSlice Constructor begin ======================");
            DisplayMetrics dm = context.getResources().getDisplayMetrics();
            KLog.i("density:" + dm.density + "  api:" + dm.densityDpi);
            KLog.i("screenRect:" + screenRect);
            KLog.i("adView local visible Rect:" + selfRect);
            KLog.i("[2t] captureTime:" + captureTime);
            KLog.i("[2k] adView Size:" + adSize);
            KLog.i("[2d] adView visible left top Point:" + visibleLeftPoint);
            KLog.i("[2l] adView alpha:" + alpha);
            KLog.i("[2m] adView isShown:" + shown);
            KLog.i("[2o] adView visible Size:" + visibleSize);
            KLog.i("[2n] adView cover rate:" + coverRate);
            KLog.i("[2r] current Screen is Light:" + screenOn);
            KLog.i("[2s] adView is forground:" + isForground);
            KLog.i("[2f] current adView visible ability:" + visibleAbility);
            KLog.i("checkFrameBounds:" + checkFrameBounds);
            KLog.i("adView isIntersets :" + isIntersets + "    overlapRect:" + overlapRect);
            KLog.i("adView window origin Rect:" + visibleRect);

            KLog.i("=================ViewFrameSlice Constructor end ======================");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public long getCaptureTime() {
        return captureTime;
    }

    public String getAdSize() {
        return adSize;
    }

    public String getVisiblePoint() {
        return visiblePoint;
    }

    public String getVisibleSize() {
        return visibleSize;
    }

    public float getAlpha() {
        return alpha;
    }

    public int getShown() {
        return shown;
    }

    public int getScreenOn() {
        return screenOn;
    }

    public int getIsForGround() {
        return isForground;
    }

    public double getCoverRate() {
        return coverRate;
    }



    /**
     * 结合参数判断AdView是否可视化
     *
     * @return
     */
    public boolean validateAdVisible(float confCoverRate) {
        //被覆盖率 <= 0.5 && 显示 && 不完全透明 && 开屏
        if (coverRate <= confCoverRate && shown == 1 && alpha > 0.001 && screenOn == 1 && isForground == 1) {
            visibleAbility = 1;
        } else {
            visibleAbility = 0;
        }
        return (visibleAbility == 1);
    }

    /**
     * 遍历对比父视图以及同层级下的子视图
     *
     * @param adView 广告视图
     * @param
     */
    private void traverseParent(View adView) {

        View contentView = adView;

        // 从当前view找到父类布局容器，然后逐层向上(superview)遍历
        while (contentView.getParent() instanceof ViewGroup) {
            // 父类容器
            ViewGroup currentParent = (ViewGroup) contentView.getParent();
            // getClipBounds() require API23

            int start = indexOfViewInParent(contentView, currentParent);
            int childCount = currentParent.getChildCount();
//            System.out.println("indexOfViewInParent:" + start + "  parent`s child count:" + childCount);
            // 当前视图的下一个index和adview对比frame是否有交叉
            for (int i = start + 1; i < childCount; i++) {

                View otherView = currentParent.getChildAt(i);

                //checkFrameBounds(otherView);
                // 如果相邻的视图(容器)是隐藏、透明、或是frame=0，则不再与adview进行对比
                boolean itemShow = isSubViewNormal(otherView);



                if (!itemShow) {
                    isWindowShowed = itemShow;// 赋值全局变量
//                   System.out.println("index:" + i + " view:" + otherView + "  is`t showing:" + itemShow);
                    continue;
                }
                checkIntersects(adView, otherView);
//
//			// 遍历的view如果是个布局容器，则继续深度遍历其子view是否和adview有交集
                if (otherView instanceof ViewGroup) {
                    // traverseParent(otherView);
                    ViewGroup subviewGroup = (ViewGroup) otherView;
                    traverseSubviews(adView, subviewGroup, coverFrameSet);
                }

            }
            // 当前parent视图赋值给contentView，开始下一个遍历
            contentView = currentParent;
//            debugD("*************************************************************");
        }

    }


    /**
     * 遍历右半子树上布局容器下的所有view是否和目标view有交集
     * @param adview 目标view
     * @param subview 容器下的所有view
     */
    public void traverseSubviews(View adview, ViewGroup subview, Set<String> coverList) {
        // int idex = indexOfViewInParent(adview, subview);
        int childCount = subview.getChildCount();

//        debugD("@@@@@@@@@@@@@@@@@@@@@" + subview.toString()  );
//        System.out.println("traverseSubviews:" + subview + "  child count:" + childCount);

        // 遍历当前视图(容器)的所有子视图，对比目标view的frame是否有交叉
        for (int i = 0; i < childCount; i++) {
            View otherView = subview.getChildAt(i);

//            debugD("^^^^^^^^^^^^^^^^^^^^^^^^^" + otherView.toString()  );


            //checkFrameBounds(otherView);
            // 如果相邻的视图(容器)是隐藏、透明、或是frame=0，则不再与adview进行对比
            boolean isShowed = isSubViewNormal(otherView);
            if (!isShowed) {
//                debugE("subview index:" + i + " view:" + otherView + "  is`t showing:");
                continue;
            }

            checkIntersects(adview, otherView);

            // 子视图又是容器，则继续深度遍历，直至最底层
            if (otherView instanceof ViewGroup) {
                ViewGroup subviewGroup = (ViewGroup) otherView;
                traverseSubviews(adview, subviewGroup, coverList);
            }
        }

    }



    /**
     * 检查广告视图和对比目标视图是否有覆盖
     * @param adView
     * @param otherView
     */
    @SuppressLint("NewApi")
    private void checkIntersects(View adView, View otherView) {
//        debugD("#############" + adView.toString() + "########" + otherView.toString());

        // 固定的广告视图的frame
        Rect viewRect = new Rect();
        adView.getGlobalVisibleRect(viewRect);
        // 当前index下视图的frame
        Rect otherViewRect = new Rect();
        Point viewOffset = new Point();
        otherView.getGlobalVisibleRect(otherViewRect, viewOffset);

        //通过对比view的layout和其父layout，判断是否允许剪切超出视图区域部分，默认是判断，以便在4.4.2版本下如果
        ViewGroup currentGroup = null;
        ViewGroup fatherGroup = null;
        boolean isClipChildren = true;
        if (Build.VERSION.SDK_INT >= 18) {// above 4.4.2
            if (otherView.getParent() instanceof ViewGroup) {
//                debugD("#####是容器" + adView.toString() + "########" + otherView.toString());
                currentGroup = (ViewGroup) otherView.getParent();
                if (currentGroup != null && currentGroup.getParent() instanceof ViewGroup) {
//                    debugD("#####再进入是容器" + adView.toString() + "########" + otherView.toString());

                    fatherGroup = (ViewGroup) currentGroup.getParent();
                    if (fatherGroup != null) {
                        isClipChildren = fatherGroup.getClipChildren();
                        //debugE("isClipChildren:"+isClipChildren);
                    }
                }
            }
        }
        if (!isClipChildren) {
            // 如果view有相对自身偏移量(margin_TOP)，则加上偏移量进行计算覆盖区域
            //otherViewRect.left += (viewOffset.x - otherViewRect.left);//=viewOffset.x
            //otherViewRect.top += (viewOffset.y - otherViewRect.top);//=viewOffset.y
            otherViewRect.left = viewOffset.x;
            otherViewRect.top = viewOffset.y;
        }

        //如果view的父视图(layout)有偏移量(margin OR Panding)，则计算实际可视覆盖区域 rect+localRect+父容器panding
        //TEST 目前有问题，当父layout之间有约束时，即便设置了相关panding属性，但是由于各种约束，panding不会生效，导致计算出来的frame还是有问题
        int pandingLeft = 0;
        int pandingTop = 0;
        if (otherView.getParent() instanceof ViewGroup) {
            ViewGroup currentParent = (ViewGroup) otherView.getParent();
            pandingLeft = currentParent.getPaddingLeft();
            pandingTop = currentParent.getPaddingTop();
        }
        if (pandingLeft != 0 || pandingTop != 0) {
            Rect localRect = new Rect();
            // view相对自身的位置，一般为左上角0,0;右下角width,height
            otherView.getLocalVisibleRect(localRect);
            otherViewRect.left += (localRect.left + pandingLeft);
            otherViewRect.top += (localRect.top + pandingTop);
//            System.out.println("visualLeft:" + otherViewRect.left + "  visualTop:" + otherViewRect.top);
        }

        // 是否相交a.left < b.right && b.left < a.right && a.top < b.bottom && b.top < a.bottom

        boolean isIntersets = Rect.intersects(viewRect, otherViewRect);
//
//        debugV(" checkIntersects:" + viewRect + "  otherView:" + otherView + "  child`rect:" + otherViewRect
//                + "  intersects:" + isIntersets + "   isViewgroup:" + (otherView instanceof ViewGroup));

        // 如果对比的两个视图有交叉或覆盖，交叉或覆盖的部分存储到list内
        if (isIntersets) {

//            debugD("#############" + adView.toString() + "########" + otherView.toString());

            valOverlapRect(viewRect, otherViewRect);


        }

    }


    /**
     * 通过两个视图的Rect获取相交区域的frame
     * @param adRect 广告视图
     * @param otherRect 子视图
     */
    private void valOverlapRect(Rect adRect, Rect otherRect) {
        // 方法一
        int overlapWidth = 0;
        int overlapHeight = 0;
        int overlapLeft = 0;
        int overlapRight = 0;
        int overlapTop = 0;
        int overlapBottom = 0;

        // 坐标系左上坐标x点
        overlapLeft = Math.max(adRect.left, otherRect.left);
        // 坐标系左上坐标y点
        overlapTop = Math.max(adRect.top, otherRect.top);
        // 坐标系右下坐标x点
        overlapRight = Math.min(adRect.right, otherRect.right);
        // 坐标系右下坐标y点
        overlapBottom = Math.min(adRect.bottom, otherRect.bottom);

        overlapWidth = Math.abs(overlapRight - overlapLeft);
        overlapHeight = Math.abs(overlapBottom - overlapTop);

        StringBuilder item = new StringBuilder();
        item.append(overlapLeft);
        item.append(Constant.DIVIDE_MULT);
        item.append(overlapTop);
        item.append(Constant.DIVIDE_MULT);
        item.append(overlapWidth);
        item.append(Constant.DIVIDE_MULT);
        item.append(overlapHeight);
        // 存储jsondata:覆盖视图的x,y和width,height
        coverFrameSet.add(item.toString()); // (leftXtopXwidthXheight)

        // 存储RECT
        Rect overlapRect = new Rect();
        boolean success = overlapRect.setIntersect(adRect, otherRect);

        Rectangle rect = new Rectangle();
        rect.x1 = overlapRect.left;
        rect.y1 = overlapRect.top;
        rect.x2 = overlapRect.right;
        rect.y2 = overlapRect.bottom;

        // 如果success,overlapRect的left,top,right,bottom便是相交区域的bounds
//        debugE("valOverlapRect:[" + item.toString() + "]   " + success + "   voerlap:" + overlapRect+"   rect:"+rect);

        coverAreaList.add(rect);
    }

    private boolean isSubViewNormal(View contentView) {
        Rect currentViewRect = new Rect();
        boolean currentVisible = contentView.getGlobalVisibleRect(currentViewRect);

        return currentVisible && checkVisibled(contentView);
    }


    /**
     * 判断当前view是否是透明或隐藏
     *
     * @param contentView
     * @return
     */
    private boolean checkVisibled(View contentView) {
        try {
            // visible&hidden
            if (!(contentView.getVisibility() == View.VISIBLE)) {
//                int id = contentView.getBackground().getAlpha();
                return false;
            }
            //判断view是否包含子view
            if (contentView instanceof ViewGroup){
                ViewGroup viewGroup = (ViewGroup)contentView;
                //未设置背景色
                if(viewGroup.getBackground() == null || viewGroup.getChildCount() < 1){
                    return false;
                }
            }
            // transparent
            if (contentView.getAlpha() <= 0.1f) {// require min api 11
                return false;
            }

        } catch (Throwable e) {
            e.printStackTrace();
        }
        return true;
    }
//    private boolean checkVisibled(View contentView) {
//        try {
//            // visible&hidden
//            if (!(contentView.getVisibility() == View.VISIBLE)) {
//                return false;
//            }
//            // transparent
//            if (contentView.getAlpha() <= 0.1f) {// require min api 11
//                return false;
//            }
//        } catch (Throwable e) {
//            e.printStackTrace();
//        }
//
//        return true;
//    }



    /**
     * 当前视图在父视图的层级位置index
     *
     * @param view
     *            当前视图
     * @param parent
     *            父视图
     * @return
     */
    private int indexOfViewInParent(View view, ViewGroup parent) {
        int index;
        int childCount = parent.getChildCount();
        for (index = 0; index < childCount; index++) {
            if (parent.getChildAt(index) == view)
                break;
        }
        return index;
    }



    /**
     * 判断adview的superview是否正常显示
     * @return
     */
    private boolean isShowing(View contentView) {
        // 当前广告视图是否被裁减&&当前广告视图是透明或隐藏
//			return checkFrameBounds(contentView) && checkVisibled(contentView);
        Rect currentViewRect = new Rect();
        Point offsets = new Point();
        // 如果contentView的visibility=gone或width,height都为0，返回为false(Rect=(0,0,0,0))
        boolean currentVisible = contentView.getGlobalVisibleRect(currentViewRect,offsets);// left,top,right,bottom

        return currentVisible;

    }



    /**
     * 判断两个Slice是否相同
     *
     * @param otherSlice
     * @return
     */
    public boolean isSameAs(ViewFrameSlice otherSlice) {
        try {
            if (adSize.equals(otherSlice.adSize)
                    && visiblePoint.equals(otherSlice.visiblePoint)
                    && visibleSize.equals(otherSlice.visibleSize)
                    && Math.abs(alpha - otherSlice.alpha) < 0.001
                    && shown == otherSlice.shown
                    && screenOn == otherSlice.screenOn
                    && isForground == otherSlice.isForground
                    && coverRate == otherSlice.coverRate) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean checkFrameBounds(View contentView) {
        try {

            Rect currentViewRect = new Rect();
            Point offsets = new Point();
            // 如果contentView的visibility=gone或width,height都为0，返回为false(Rect=(0,0,0,0))
            boolean currentVisible = contentView.getGlobalVisibleRect(currentViewRect, offsets);// left,top,right,bottom

            boolean heightVisible = (currentViewRect.bottom - currentViewRect.top) >= contentView.getMeasuredHeight();
            boolean widthVisible = (currentViewRect.right - currentViewRect.left) >= contentView.getMeasuredWidth();
            boolean totalViewVisible = currentVisible && heightVisible && widthVisible;

            //KLog.i("checkFrameBounds,rect:" + currentViewRect + "  offset:" + offsets + "  height:" + contentView.getMeasuredHeight() + "  width:" + contentView.getMeasuredWidth());
            //KLog.i("checkFrameBounds,current:" + currentVisible + "  heightVisiable:" + heightVisible + "  widthVisiable:" + widthVisible);

            if (!totalViewVisible)
                return false;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    @SuppressLint("NewApi")
	private Rect traverseParent(View adView, Rect originRect) {

        View currentView = adView;
        Rect overlapRect = originRect;

        try {
            while (currentView.getParent() instanceof ViewGroup) {

                // 父类容器
                ViewGroup currentParent = (ViewGroup) currentView.getParent();
                Rect parentRect = new Rect();
                currentParent.getGlobalVisibleRect(parentRect);

                //父父容器
                ViewGroup superParent = null;
                if (currentParent.getParent() instanceof ViewGroup) {
                    superParent = (ViewGroup) currentParent.getParent();
                }

                //默认为true 裁减子视图边框
                boolean clipChildRen = false;
                if (superParent != null && Build.VERSION.SDK_INT > 18) {
                    clipChildRen = superParent.getClipChildren();
                }

                // KLog.w("current:" + currentParent + "  super:" + superParent + "   clipChildRen:" + clipChildRen);

                //如果当前容器超出区域不被剪切
                if (clipChildRen) {

                    Rect rect = new Rect();

                    boolean isIntersets = rect.setIntersect(overlapRect, parentRect);
                    overlapRect = rect;

                    //int vWidth = Math.abs(rect.right - rect.left);
                    //int vHeight = Math.abs(rect.bottom - rect.top);
                    //KLog.v("isIntersets:" + isIntersets + "   overlapRect:" + rect + "  vWidth:" + vWidth + "  vHeight:" + vHeight);
                }

                currentView = currentParent;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        return overlapRect;
    }


    @SuppressLint("NewApi")
	@SuppressWarnings("unused")
    private Rect traverseRootView(View adView, Rect originRect) {
        ArrayList<ViewGroup> viewGroups = new ArrayList<>();
        View currentView = adView;
        Rect overlapRect = originRect;
        try {
            while (currentView.getParent() instanceof ViewGroup) {
                ViewGroup currentParent = (ViewGroup) currentView.getParent();

                viewGroups.add(currentParent);

                currentView = currentParent;
            }


            int len = viewGroups.size();

            //从最定层开始遍历
            for (int i = len - 1; i >= 0; i--) {
                ViewGroup item = viewGroups.get(i);
                Rect parentRect = new Rect();
                item.getGlobalVisibleRect(parentRect);

                //默认为true 裁减子视图边框
                boolean clipChildRen = true;
                if (Build.VERSION.SDK_INT > 18) {
                    clipChildRen = item.getClipChildren();
                    //KLog.e("current:" + item + "  rect:" + parentRect + "   clipChildRen:" + clipChildRen);
                }

                if (clipChildRen) {
                    if ((i - 1) >= 0) {
                        ViewGroup subItem = viewGroups.get(i - 1);
                        Rect itemRect = new Rect();
                        subItem.getGlobalVisibleRect(itemRect);
                        Rect rect = new Rect();

                        boolean isIntersets = rect.setIntersect(overlapRect, itemRect);

                        int vWidth = Math.abs(rect.right - rect.left);
                        int vHeight = Math.abs(rect.bottom - rect.top);

                        overlapRect = rect;
                        //KLog.v("isIntersets:" + isIntersets + "   overlapRect:" + rect + "  vWidth:" + vWidth + "  vHeight:" + vHeight);
                    }
                }

            }


        } catch (Exception e) {
            e.printStackTrace();
        }

        return overlapRect;

    }

    @Override
    public String toString() {
        return "[ 2t=" + captureTime + ",2k=" + adSize + ",2d=" + visiblePoint + ",2o=" + visibleSize + ",2n=" + coverRate + ",2l=" + alpha + ",2m=" + shown + ",2r=" + screenOn + ",2s=" + isForground + "]";
    }
}



