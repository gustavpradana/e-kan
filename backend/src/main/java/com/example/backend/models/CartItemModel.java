package com.example.backend.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "cart_item")
public class CartItemModel {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(unique = true)
    private UUID id_cart ;

    @Column
    private int jumlah_item = 1;

    @Column
    private Boolean is_checked = false ;

    @ManyToOne
    @JoinColumn(name = "penjual_id")
    private PenjualModel penjual ;

    @ManyToOne
    @JoinColumn(name = "pembeli_id")
    private PembeliModel pembeli ;

}
