package com.miguelbtcode.springboot.webflux.app.controllers;

import com.miguelbtcode.springboot.webflux.app.models.documents.Producto;
import com.miguelbtcode.springboot.webflux.app.models.services.ProductoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.thymeleaf.spring6.context.webflux.ReactiveDataDriverContextVariable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;


@SessionAttributes("producto")
@Controller
public class ProductoController {

    @Autowired
    private ProductoService service;
    private static final Logger log = LoggerFactory.getLogger(ProductoController.class);
    @GetMapping({"/listar", "/"})
    public Mono<String> listar(Model model){
        Flux<Producto> productos = service.findAllWithNameToUpperCase();

        productos.subscribe(producto -> log.info(producto.getNombre()));

        model.addAttribute("productos", productos);
        model.addAttribute("titulo", "Listado de productos");
        return Mono.just("listar");
    }

    @GetMapping("/form")
    public Mono<String> crear(Model model){
        model.addAttribute("producto", new Producto());
        model.addAttribute("titulo", "formulario de producto");
        model.addAttribute("boton", "Crear");
        return Mono.just("form");
    }

    @GetMapping("/form-v2/{id}")
    public Mono<String> editarV2(@PathVariable String id, Model model){

        return service.findById(id)
                .doOnNext(p -> {
                    model.addAttribute("boton", "Editar");
                    model.addAttribute("titulo", "Editar producto");
                    model.addAttribute("producto", p);

                    log.info("Producto: " + p.getNombre());
                })
                .defaultIfEmpty(new Producto())
                .flatMap(p -> {
                    if (p.getId() == null){
                        return Mono.error(new InterruptedException("No existe el producto"));
                    }
                    return Mono.just(p);
                })
                .then(Mono.just("form"))
                .onErrorResume(ex -> Mono.just("redirect:/listar?error=no+existe+el+producto"));
    }

    @GetMapping("/form/{id}")
    public Mono<String> editar(@PathVariable String id, Model model){
        Mono<Producto> productoMono = service.findById(id)
                .doOnNext(p -> {
                    log.info("Producto: " + p.getNombre());
                })
                .defaultIfEmpty(new Producto());

        model.addAttribute("titulo", "Editar producto");
        model.addAttribute("producto", productoMono);

        return Mono.just("form");
    }

    @PostMapping("/form")
    public Mono<String> guardar(Producto producto, SessionStatus status){
        status.setComplete();
        return service.save(producto).doOnNext(p -> {
            log.info("Producto guardado: " + p.getNombre() + " Id: " + p.getId());
        }).thenReturn("redirect:/listar");
    }

    @GetMapping("/listar-datadriver")
    public String listarDataDriver(Model model){
        Flux<Producto> productos = service.findAllWithNameToUpperCase().delayElements(Duration.ofSeconds(1));

        productos.subscribe(producto -> log.info(producto.getNombre()));

        model.addAttribute("productos", new ReactiveDataDriverContextVariable(productos, 2));
        model.addAttribute("titulo", "Listado de productos");
        return "listar";
    }

    @GetMapping("/listar-full")
    public String listarFull(Model model){
        Flux<Producto> productos = service.findAllWithNameToUpperCaseAndRepeat();

        model.addAttribute("productos", productos);
        model.addAttribute("titulo", "Listado de productos");
        return "listar";
    }

    @GetMapping("/listar-chunked")
    public String listarChunked(Model model){
        Flux<Producto> productos = service.findAllWithNameToUpperCaseAndRepeat();

        model.addAttribute("productos", productos);
        model.addAttribute("titulo", "Listado de productos");
        return "listar-chunked";
    }
}
