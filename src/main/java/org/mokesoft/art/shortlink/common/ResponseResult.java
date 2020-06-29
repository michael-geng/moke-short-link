package org.mokesoft.art.shortlink.common;

/**
 * 接口返回数据统一模板
 */
public class ResponseResult<T> {

    /**
     * 0表示正常
     */
    public static final int SUCCESS_CODE = 0;

    public static final int ERROR_CODE = 1;

    public static final String SUCCESS_MSG = "success";

    public static final String ERROR_MSG = "error";

    public static final String SIGN_ERROR_MSG = "SIGN_ERROR";

    public static final ResponseResult SUCCESS = new ResponseResult(SUCCESS_CODE, SUCCESS_MSG);

    public static final ResponseResult ERROR = new ResponseResult(ERROR_CODE, ERROR_MSG);

    private int code;

    private String msg;

    private T data;

    private Long time;

    public ResponseResult() {
        super();
    }

    public ResponseResult(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public ResponseResult(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> ResponseResult build(int code, String msg, T data) {
        return new ResponseResult(code, msg, data);
    }

    public static ResponseResult build(int code, String msg) {
        return new ResponseResult(code, msg);
    }

    public static <T> ResponseResult success(T data) {
        return new ResponseResult(SUCCESS_CODE, SUCCESS_MSG, data);
    }

    public static ResponseResult error() {
        return new ResponseResult(ERROR_CODE, ERROR_MSG);
    }

    public static ResponseResult error(String msg) {
        return new ResponseResult(ERROR_CODE, msg);
    }

    public Long getTime() {
        return System.currentTimeMillis();
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    public T getData() {
        return data;
    }
}
