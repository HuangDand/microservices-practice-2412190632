package com.zjgsu.hwy.common;

/** 依赖的远程服务当前不可用（无实例或无法建立连接）。 */
public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message) {
        super(message);
    }

    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
