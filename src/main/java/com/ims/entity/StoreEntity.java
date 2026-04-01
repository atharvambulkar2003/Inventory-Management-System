package com.ims.entity;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.ToString;

@Entity
@Table(name = "stores")
@Data
@ToString(exclude = {"owner", "staff", "products"})
public class StoreEntity {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	
	@Column(nullable = false)
	private String storeName;
	
	@Column(unique = true)
    private String gstNumber;
    
    @OneToOne
    @JoinColumn(name = "owner_id")
    private UserEntity owner;
    
    @OneToMany(mappedBy = "store", cascade = CascadeType.ALL)
    private List<ProductEntity> products;
    
    @OneToMany(mappedBy = "store", cascade = CascadeType.ALL)
    private List<UserEntity> staff;
}
