package com.ssafy.gourming.util;

public final class ValidationConstants {
    
    private ValidationConstants() {
        // 인스턴스화 방지
    }

    public static final String HANDLE_REGEX = "^@[a-zA-Z0-9_.]{4,20}$";
    public static final String HANDLE_MESSAGE = "핸들은 @로 시작하고 영문, 숫자, _, .만 사용할 수 있습니다 (4~20자)";
}
