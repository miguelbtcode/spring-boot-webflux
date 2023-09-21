package com.miguelbtcode.springboot.webflux.app.models.services;

import com.miguelbtcode.springboot.webflux.app.models.dao.ProductoDao;
import com.miguelbtcode.springboot.webflux.app.models.documents.Producto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ProductoServiceImpl implements ProductoService {

    @Autowired
    private ProductoDao dao;

    @Override
    public Flux<Producto> findAll() {
        return dao.findAll();
    }

    @Override
    public Flux<Producto> findAllWithNameToUpperCase() {
        return dao.findAll().map(producto -> {
            producto.setNombre(producto.getNombre().toUpperCase());
            return producto;
        });
    }

    @Override
    public Flux<Producto> findAllWithNameToUpperCaseAndRepeat() {
        return findAllWithNameToUpperCase().repeat(5000);
    }

    @Override
    public Mono<Producto> findById(String id) {
        return dao.findById(id);
    }

    @Override
    public Mono<Producto> save(Producto producto) {
        return dao.save(producto);
    }

    @Override
    public Mono<Void> delete(Producto producto) {
        return dao.delete(producto);
    }
}
