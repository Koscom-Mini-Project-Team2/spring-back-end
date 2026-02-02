package koscom.mini_project.team2.team2.util;

import java.lang.reflect.Field;
import java.util.List;

public class Utils {

    public static String convertListToString(List<?> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder();

        for (Object obj : list) {
            sb.append("- ");

            Field[] fields = obj.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                try {
                    Object value = field.get(obj);
                    sb.append(field.getName())
                            .append("=")
                            .append(value)
                            .append(", ");
                } catch (IllegalAccessException e) {
                    // 접근 불가 필드는 무시
                }
            }

            // 마지막 ", " 제거
            if (sb.length() >= 2) {
                sb.setLength(sb.length() - 2);
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}

