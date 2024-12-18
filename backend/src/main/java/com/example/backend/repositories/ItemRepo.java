package com.example.backend.repositories;

import com.example.backend.models.ItemModel;
import com.example.backend.models.PenjualModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface ItemRepo extends JpaRepository<ItemModel , UUID> {
    Optional<List<ItemModel>> findByPenjual (PenjualModel model);
}
