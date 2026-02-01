package koscom.mini_project.team2.team2.domain.dummy.dto;

import koscom.mini_project.team2.team2.domain.dummy.entity.Dummy;

public record DummyResponse(
        Long id,
        String name,
        Integer age
) {
    public static DummyResponse from(Dummy dummy) {
        return new DummyResponse(dummy.getId(), dummy.getName(), dummy.getAge());
    }
}
