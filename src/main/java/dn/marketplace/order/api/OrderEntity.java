package dn.marketplace.order.api;


import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(schema = "market_place",name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class OrderEntity {
}
