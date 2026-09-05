package dn.marketplace.product.api;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(schema = "market_place",name = "products")
@Getter
@Setter
@NoArgsConstructor
public class ProductEntity {
}
