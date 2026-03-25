package com.ims.entity;

import java.time.LocalDateTime;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "users")
@Data
public class UserEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(nullable = false, unique = true)
    private String username;
	private String fullName;
    private String password; 
    @Column(nullable = false,unique = true)
    private String email;
    private String role;
    private boolean active = true; 
    @ManyToOne
    @JoinColumn(name = "store_id")
    private StoreEntity store;
    
    private String otp; 
    private LocalDateTime otpExpiryTime;
}
