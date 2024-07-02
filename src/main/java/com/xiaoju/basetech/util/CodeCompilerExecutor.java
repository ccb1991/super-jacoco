package com.xiaoju.basetech.util;

import com.xiaoju.basetech.entity.CoverageReportEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static com.xiaoju.basetech.util.Constants.LOG_PATH;


/**
 * @description:
 * @author: charlynegaoweiwei
 * @time: 2020/4/28 4:59 下午
 */
@Component
public class CodeCompilerExecutor {

    public void compileCode(CoverageReportEntity coverageReport) throws IOException {
        String logFilePath = coverageReport.getLogFile().replace(LocalIpUtils.getTomcatBaseUrl()+"logs/", LOG_PATH);
        String[] compileCmd = new String[]{"cd " + coverageReport.getNowLocalPath() + " && mvn clean compile " +
                (StringUtils.isEmpty(coverageReport.getEnvType()) ? "" : "-P=" + coverageReport.getEnvType()) + ">>" + logFilePath};
//        File logFile=new File(logFilePath);
//        if (!logFile.exists()){
//            boolean success=logFile.createNewFile();
//            if (!success){throw new IOException("日志文件创建失败");};
//        }
//        String[] compileCmd = new String[]{"cd " + coverageReport.getNowLocalPath() ,"mvn clean compile",
//                (StringUtils.isEmpty(coverageReport.getEnvType()) ? "" : "-P=" + coverageReport.getEnvType()),
//                        ">>" + logFilePath};
//        List<String> compileCmd=new ArrayList<>();
//        compileCmd.add("cd " + coverageReport.getNowLocalPath());
//        if (!StringUtils.isEmpty(coverageReport.getEnvType()) ){
//            compileCmd.add("mvn clean compile");
//            compileCmd.add("-P=" + coverageReport.getEnvType()+">>" + logFile);
//        } else {
//            compileCmd.add("mvn clean compile");
//            compileCmd.add("mvn clean compile"+">>" + logFile);
//        }
        try {
//            int exitCode = CmdExecutor.executeCmd(compileCmd.toArray(new String[0]), 600000L);
            int exitCode = CmdExecutor.executeCmd(compileCmd , 600000L);
            if (exitCode != 0) {
                coverageReport.setRequestStatus(Constants.JobStatus.COMPILE_FAIL.val());
                coverageReport.setErrMsg("编译代码出错");
            } else {
                coverageReport.setRequestStatus(Constants.JobStatus.COMPILE_DONE.val());
            }
        } catch (TimeoutException e) {
            coverageReport.setRequestStatus(Constants.JobStatus.COMPILE_FAIL.val());
            coverageReport.setErrMsg("编译代码超过了10分钟");
        } catch (Exception e) {
            coverageReport.setErrMsg("编译代码发生未知错误");
            coverageReport.setRequestStatus(Constants.JobStatus.COMPILE_FAIL.val());
        }
    }

}