package sfedu.ictis.woi.model.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Статус точки интереса")
public enum PoiStatus {
    @Schema(description = "Ожидает проверки администратора")
    PENDING,

    @Schema(description = "Опубликована и доступна пользователям")
    APPROVED,

    @Schema(description = "Отклонена администратором (удалено)")
    REJECTED
}