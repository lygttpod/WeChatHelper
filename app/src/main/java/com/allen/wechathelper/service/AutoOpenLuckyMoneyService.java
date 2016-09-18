package com.allen.wechathelper.service;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.app.PendingIntent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.allen.wechathelper.commom.UI;

import java.util.List;

/**
 * Created by allen on 2016/9/18.
 */
public class AutoOpenLuckyMoneyService extends AccessibilityService {

    private static final String TAG = AutoOpenLuckyMoneyService.class.getSimpleName();
    private static final String MyTAG = "allen";

    private static final int MSG_BACK_HOME = 0;
    private static final int MSG_BACK_ONCE = 1;
    boolean hasNotify = false;
    boolean hasLuckyMoney = true;



    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int eventType = event.getEventType(); // 事件类型
        printEventLog(event);//查看log日志

        switch (eventType){
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED: // 通知栏事件
                Log.i(TAG, "TYPE_NOTIFICATION_STATE_CHANGED");
                openAppByNotification(event);
                hasNotify = true;
                break;

            default:
                Log.i(TAG, "DEFAULT");
                if(hasNotify) {
                    AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                    clickLuckyMoney(rootNode);

                    String className = event.getClassName().toString();
                    if (className.equals(UI.LUCKY_MONEY_RECEIVE_UI)) {
                        if(!openLuckyMoney()) {
                            backToHome();
                            hasNotify = false;
                        }
                        hasLuckyMoney = true;
                    } else if (className.equals(UI.LUCKY_MONEY_DETAIL_UI)) {
                        backToHome();
                        hasNotify = false;
                        hasLuckyMoney = true;
                    } else {
                        if(!hasLuckyMoney) {
                            handler.sendEmptyMessage(MSG_BACK_ONCE);
                            hasLuckyMoney = true;   // 防止后退多次
                        }
                    }
                }
                break;

        }
    }

    @Override
    public void onInterrupt() {

    }

    /**
     * 打开微信
     * @param event 事件
     */
    private void openAppByNotification(AccessibilityEvent event) {
        if (event.getParcelableData() != null  && event.getParcelableData() instanceof Notification) {
            Notification notification = (Notification) event.getParcelableData();
            try {
                PendingIntent pendingIntent = notification.contentIntent;
                pendingIntent.send();
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
//            }
        }
    }

    private void clickLuckyMoney(AccessibilityNodeInfo rootNode) {
        if(rootNode != null) {
            int count = rootNode.getChildCount();
            for (int i = count - 1; i >= 0; i--) {  // 倒序查找最新的红包
                AccessibilityNodeInfo node = rootNode.getChild(i);
                if (node == null)
                    continue;

                CharSequence text = node.getText();
                if (text != null && text.toString().equals("领取红包")) {
                    AccessibilityNodeInfo parent = node.getParent();
                    while (parent != null) {
                        if (parent.isClickable()) {
                            parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            break;
                        }
                        parent = parent.getParent();
                    }
                }

                clickLuckyMoney(node);
            }
        }
    }

    private boolean openLuckyMoney() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if(rootNode != null) {
            List<AccessibilityNodeInfo> nodes =
                    rootNode.findAccessibilityNodeInfosByViewId(UI.OPEN_LUCKY_MONEY_BUTTON_ID);
            for(AccessibilityNodeInfo node : nodes) {
                if(node.isClickable()) {
                    Log.i(TAG, "open LuckyMoney");
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    return true;
                }
            }
        }

        return false;
    }

    private void backToHome() {
        if(handler.hasMessages(MSG_BACK_HOME)) {
            handler.removeMessages(MSG_BACK_HOME);
        }
        handler.sendEmptyMessage(MSG_BACK_HOME);
    }


    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if(msg.what == MSG_BACK_HOME) {
                performGlobalAction(GLOBAL_ACTION_BACK);
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        performGlobalAction(GLOBAL_ACTION_BACK);
                        hasLuckyMoney = false;
                    }
                }, 1500);
            } else if(msg.what == MSG_BACK_ONCE) {
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "click back");
                        performGlobalAction(GLOBAL_ACTION_BACK);
                        hasLuckyMoney = false;
                        hasNotify = false;
                    }
                }, 1500);
            }
        }
    };

    private void printEventLog(AccessibilityEvent event) {
        Log.i(MyTAG, "-------------------------------------------------------------");
        int eventType = event.getEventType(); //事件类型
        Log.i(MyTAG, "PackageName:" + event.getPackageName() + ""); // 响应事件的包名
        Log.i(MyTAG, "Source Class:" + event.getClassName() + ""); // 事件源的类名
        Log.i(MyTAG, "Description:" + event.getContentDescription()+ ""); // 事件源描述
        Log.i(MyTAG, "Event Type(int):" + eventType + "");

        switch (eventType) {
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:// 通知栏事件
                Log.i(MyTAG, "event type:TYPE_NOTIFICATION_STATE_CHANGED");
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED://窗体状态改变
                Log.i(MyTAG, "event type:TYPE_WINDOW_STATE_CHANGED");
                break;
            case AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED://View获取到焦点
                Log.i(MyTAG, "event type:TYPE_VIEW_ACCESSIBILITY_FOCUSED");
                break;
            case AccessibilityEvent.TYPE_GESTURE_DETECTION_START:
                Log.i(MyTAG, "event type:TYPE_VIEW_ACCESSIBILITY_FOCUSED");
                break;
            case AccessibilityEvent.TYPE_GESTURE_DETECTION_END:
                Log.i(MyTAG, "event type:TYPE_GESTURE_DETECTION_END");
                break;
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                Log.i(MyTAG, "event type:TYPE_WINDOW_CONTENT_CHANGED");
                break;
            case AccessibilityEvent.TYPE_VIEW_CLICKED:
                Log.i(MyTAG, "event type:TYPE_VIEW_CLICKED");
                break;
            case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
                Log.i(MyTAG, "event type:TYPE_VIEW_TEXT_CHANGED");
                break;
            case AccessibilityEvent.TYPE_VIEW_SCROLLED:
                Log.i(MyTAG, "event type:TYPE_VIEW_SCROLLED");
                break;
            case AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED:
                Log.i(MyTAG, "event type:TYPE_VIEW_TEXT_SELECTION_CHANGED");
                break;
            default:
                Log.i(MyTAG, "no listen event");
        }

        for (CharSequence txt : event.getText()) {
            Log.i(MyTAG, "text:" + txt);
        }

        Log.i(MyTAG, "-------------------------------------------------------------");
    }
}
