package com.allen.wechathelper.service;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.allen.wechathelper.commom.UI;

import java.util.List;

/**
 * Created by allen on 2016/9/18.
 */
public class AutoReplyService extends AccessibilityService {

    private static final String TAG = AutoReplyService.class.getSimpleName();
    private static final String MyTAG = "allen";

    private Handler handler = new Handler();
    private boolean hasNotify = false;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int eventType = event.getEventType(); // 事件类型

        printEventLog(event);//查看log日志

        switch (eventType){
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED: // 通知栏事件
                openAppByNotification(event);
                hasNotify = true;
                break;
            default:
                Log.i(TAG, "DEFAULT");
                if (hasNotify) { // 如果有通知
                    try {
                        Thread.sleep(1000); // 停1秒, 否则在微信主界面没进入聊天界面就执行了fillInputBar
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (fillInputBar("我在敲代码，稍后回复哈~")) { // 找到输入框，即EditText
                        findAndPerformAction(UI.BUTTON, "发送"); // 点击发送
                        handler.postDelayed(new Runnable() { // 返回主界面，这里延迟执行，为了有更好的交互
                            @Override
                            public void run() {
                                performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);   // 返回
                            }
                        }, 1500);

                    }
                    hasNotify = false;
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
        }
    }

    /**
     * 查找UI控件并点击
     * @param widget 控件完整名称, 如android.widget.Button, android.widget.TextView
     * @param text 控件文本
     */
    private void findAndPerformAction(String widget, String text) {
        // 取得当前激活窗体的根节点
        if (getRootInActiveWindow() == null) {
            return;
        }

        // 通过文本找到当前的节点
        List<AccessibilityNodeInfo> nodes = getRootInActiveWindow().findAccessibilityNodeInfosByText(text);
        if(nodes != null) {
            for (AccessibilityNodeInfo node : nodes) {
                if (node.getClassName().equals(widget) && node.isEnabled()) {
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK); // 执行点击
                    break;
                }
            }
        }
    }

    /**
     * 填充输入框
     */
    private boolean fillInputBar(String reply) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode != null) {
            return findInputBar(rootNode, reply);
        }
        return false;
    }

    /**
     * 查找EditText控件
     * @param rootNode 根结点
     * @param reply 回复内容
     * @return 找到返回true, 否则返回false
     */
    private boolean findInputBar(AccessibilityNodeInfo rootNode, String reply) {
        int count = rootNode.getChildCount();
        Log.i(TAG, "root class=" + rootNode.getClassName() + ", " + rootNode.getText() + ", child: " + count);
        for (int i = 0; i < count; i++) {
            AccessibilityNodeInfo node = rootNode.getChild(i);
            if (UI.EDITTEXT.equals(node.getClassName())) {   // 找到输入框并输入文本
                Log.i(TAG, "****found the EditText");
                setText(node, reply);
                return true;
            }

            if (findInputBar(node, reply)) {    // 递归查找
                return true;
            }
        }
        return false;
    }

    /**
     * 设置文本
     */
    private void setText(AccessibilityNodeInfo node, String reply) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Log.i(TAG, "set text");
            Bundle args = new Bundle();
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    reply);
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
        } else {
            ClipData data = ClipData.newPlainText("reply", reply);
            ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboardManager.setPrimaryClip(data);
            node.performAction(AccessibilityNodeInfo.ACTION_FOCUS); // 获取焦点
            node.performAction(AccessibilityNodeInfo.ACTION_PASTE); // 执行粘贴
        }
    }

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
