package koscom.mini_project.team2.team2.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import koscom.mini_project.team2.team2.domain.etf.entity.Stock;

import java.util.ArrayList;
import java.util.List;

@Converter
public class StockListJsonConverter implements AttributeConverter<List<Stock>, String> {

    private static final ObjectMapper om = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<Stock> attribute) {
        try {
            if (attribute == null) return "[]";
            return om.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new IllegalStateException("stockList -> json 변환 실패", e);
        }
    }

    @Override
    public List<Stock> convertToEntityAttribute(String dbData) {
        try {
            if (dbData == null || dbData.isBlank()) return new ArrayList<>();
            return om.readValue(dbData, new TypeReference<List<Stock>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("json -> stockList 변환 실패", e);
        }
    }
}
