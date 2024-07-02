package com.xiaoju.basetech.util;

import com.xiaoju.basetech.dao.error.NoSystemError;
import lombok.extern.log4j.Log4j;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.nio.cs.ext.GBK;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @description:
 * @author: copy from com.didichuxing.chefuqa.common.util;
 * @time: 2019/12/24 9:11 PM
 */
public class CmdExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(CmdExecutor.class);

    private static AtomicInteger counter = new AtomicInteger(0);

    private static int maxThread = 64;

    private static ThreadPoolExecutor executor = new ThreadPoolExecutor(20, maxThread, 5 * 60, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(1), r -> new Thread(r, "CmdThread-" + counter.getAndIncrement()));

    public static int executeCmd(String[] commands, Long timeout) throws Exception {
        StringBuffer ret = new StringBuffer();
        if (commands == null || commands.length == 0) {
            throw new IllegalArgumentException();
        }
        Process process = null;
        try {
            StringBuilder e = new StringBuilder();
            for (int builder = 0; builder < commands.length; ++builder) {
                e.append(commands[builder]);
                if (builder < commands.length - 1) {
                    e.append(" && ");
                }
            }
            LOG.info("CmdThreadPool:{}", executor);
            if (executor.getPoolSize() >= maxThread) {
                LOG.warn("CmdThreadPoolBusy");
            }

            LOG.info("命令行命令为：" + e);
            ProcessBuilder var12;
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
//                String[] cmd=new String[]{"cmd.exe","/C"};
//                var12= new ProcessBuilder((String[]) ArrayUtils.addAll(cmd, commands));
                var12 = new ProcessBuilder("cmd.exe","/C", e.toString());
            } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
//                String[] cmd=new String[]{"bash", "-c"};
//                var12 = new ProcessBuilder((String[]) ArrayUtils.addAll(cmd, commands));
                var12 = new ProcessBuilder("bash", "-c", e.toString());
            } else {
                throw new NoSystemError("不支持的系统类型");
            }
            var12.redirectErrorStream(true);
            process = var12.start();
            CmdExecutor.ReadLine readLine = new CmdExecutor.ReadLine(process.getInputStream(), ret, true);
            Future readLineFuture = executor.submit(readLine);
//            readLine.run();
            long begin = System.currentTimeMillis();
            if (process.waitFor(timeout, TimeUnit.MILLISECONDS)) {
                LOG.info("readLine.stop();");
                readLine.setFlag(false);
                LOG.info("progressBar.stop();");
                LOG.info("executeCmd done !!!!!!");
                LOG.info("worker done !!!!!! times = " + (System.currentTimeMillis() - begin) / 1000L + "s");
                return process.exitValue();
            } else {
                throw new TimeoutException();
            }
        } catch (IOException | InterruptedException var10) {
            LOG.error("executeCmd builder.start(); IOException : ", var10);
            throw var10;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private static class ReadLine implements Runnable {
//    private static class ReadLine  {
        private static final Logger LOGGER = LoggerFactory.getLogger("commandOutputLogger");
        private final InputStream is;
        // private final StringBuffer sb;
        private volatile boolean flag;

        private ReadLine(InputStream is, StringBuffer sb, boolean flag) {
            this.is = is;
            // this.sb = sb;
            this.flag = flag;
        }

        public void setFlag(boolean flag) {
            this.flag = flag;
        }

        @Override
        public void run() {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(this.is, "gbk"));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }

            try {
                String line;
                try {
                    while (flag && (line = reader.readLine()) != null) {
                        String e = line.trim();
                        if (e.length() != 0) {
                            LOGGER.info(e);
                            // 这个sb的太大了，另外有啥用
                            //this.sb.append(e + System.getProperty("line.separator"));
                        }
                    }
                } catch (IOException var12) {
                    LOGGER.error("@@@@@@@@@@@@@@ ReadLine Thread, read IOException : ", var12);
                }
            } finally {
                try {
                    reader.close();
                } catch (IOException var11) {
                    LOGGER.error("@@@@@@@@@@@@@@ ReadLine Thread, close IOException : ", var11);
                }

            }

        }
    }
}