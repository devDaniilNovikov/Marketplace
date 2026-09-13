package dn.marketplace.account.service;

import dn.marketplace.account.api.AccountEntity;
import dn.marketplace.account.api.dto.AccountResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * Внутренний адаптер Entity -> DTO. Package-private и лежит рядом с сервисом,
 * а не в {@code api}: сущность не должна быть частью публичной поверхности домена.
 */
@Mapper(componentModel = "spring",unmappedTargetPolicy = ReportingPolicy.ERROR)
interface AccountMapper {

    AccountResponse toResponse(AccountEntity entity);

    List<AccountResponse> toResponseList(List<AccountEntity> entities);
}
