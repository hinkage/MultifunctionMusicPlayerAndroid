package com.example.android.uamp.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by asus on 2018/5/10.
 */

public class LyricHelper {
    public static boolean isStarted = false;
    public static boolean isDownloaded = false;
    //用户保存所有的歌词和时间点信息间的映射关系的Map
    public static Map<Long, String> map = new HashMap<Long, String>();

    /**
     * 将解析得到的表示时间的字符转化为Long型
     *
     * @param timeStr 字符形式的时间点
     * @return Long形式的时间
     */
    private static long strToLong(String timeStr) {
        // 因为给如的字符串的时间格式为XX:XX.XX,返回的long要求是以毫秒为单位
        // 1:使用：分割 2：使用.分割
        String[] s = timeStr.split(":");
        int min = Integer.parseInt(s[0]);
        String[] ss = s[1].split("\\.");
        int sec = Integer.parseInt(ss[0]);
        int mill = Integer.parseInt(ss[1]);
        return min * 60 * 1000 + sec * 1000 + mill * 10;
    }

    public static void DownloadLyric(final String uri) throws IOException {
        isStarted = true;
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                isDownloaded = false;
                if (uri == null || uri.length() == 0) {
                    isStarted = false;
                    return;
                }
                // 更换歌词
                map.clear();
                long curTime = 0;
                String curContent = null;

                String asciiUri = null;
                try {
                    asciiUri = new URI(uri).toASCIIString();
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
                URL url = null;
                try {
                    url = new URL(asciiUri);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                InputStream is = null;
                try {
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    is = urlConnection.getInputStream();

                    InputStreamReader isr = new InputStreamReader(is);
                    BufferedReader reader = new BufferedReader(isr);
                    // 一行一行的读，每读一行，解析一行
                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        // 设置正则规则
                        String reg = "\\[(\\d{2}:\\d{2}\\.\\d{1,2})\\]";
                        // 编译
                        Pattern pattern = Pattern.compile(reg);
                        Matcher matcher = pattern.matcher(line);
                        // 如果存在匹配项，则执行以下操作
                        while (matcher.find()) {
                            // 得到匹配的所有内容
                            String msg = matcher.group();
                            // 得到这个匹配项开始的索引
                            int start = matcher.start();
                            // 得到这个匹配项结束的索引
                            int end = matcher.end();

                            // 得到这个匹配项中的组数
                            int groupCount = matcher.groupCount();
                            // 得到每个组中内容
                            for (int i = 0; i <= groupCount; i++) {
                                String timeStr = matcher.group(i);
                                if (i == 1) {
                                    // 将第二组中的内容设置为当前的一个时间点
                                    curTime = strToLong(timeStr);
                                }
                            }

                            // 得到时间点后的内容
                            String[] content = pattern.split(line);
                            // 输出数组内容
                            for (int i = 0; i < content.length; i++) {
                                if (i == content.length - 1) {
                                    // 将内容设置为当前内容
                                    curContent = content[i];
                                }
                            }
                            // 设置时间点和内容的映射
                            map.put(curTime, curContent);
                        }
                    }
                    isDownloaded = true;
                    isStarted = false;
                    //is.reset();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
        t.start();
    }
}