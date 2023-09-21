package com.miguelbtcode.springboot.webflux.app.models.services;

import com.miguelbtcode.springboot.webflux.app.models.documents.Producto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProductoService {

    public Flux<Producto> findAll();
    public Flux<Producto> findAllWithNameToUpperCase();
    public Flux<Producto> findAllWithNameToUpperCaseAndRepeat();
    public Mono<Producto> findById(String id);
    public Mono<Producto> save(Producto producto);
    public Mono<Void> delete(Producto producto);
}
