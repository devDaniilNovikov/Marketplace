package dn.marketplace.account.api.mapper;

import dn.marketplace.account.api.AccountEntity;
import dn.marketplace.account.api.dto.AccountListResponse;
import dn.marketplace.account.api.dto.AccountRequest;
import dn.marketplace.account.api.dto.AccountResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * Адаптер Entity -> DTO. Лежит в {@code api.mapper} и потому {@code public}:
 * из {@code service} package-private интерфейс был бы не виден. Снаружи домена
 * маппер не вызывают — межмодульные зависимости ограничивает ArchUnit.
 */
@Mapper(componentModel = "spring",unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AccountMapper {

    AccountResponse toResponse(AccountEntity entity);

    List<AccountResponse> toResponseList(List<AccountEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status",ignore = true)
    AccountEntity toEntity(AccountRequest accountRequest);

    List<AccountEntity> toEntityList(List<AccountRequest> accountRequests);

}
