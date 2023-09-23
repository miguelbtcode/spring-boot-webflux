package com.miguelbtcode.springboot.webflux.app.controllers;

import com.miguelbtcode.springboot.webflux.app.models.documents.Categoria;
import com.miguelbtcode.springboot.webflux.app.models.documents.Producto;
import com.miguelbtcode.springboot.webflux.app.models.services.ProductoService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.thymeleaf.spring6.context.webflux.ReactiveDataDriverContextVariable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;


@SessionAttributes("producto")
@Controller
public class ProductoController {

    private final ProductoService service;

    @Value("${config.uploads.path}")
    private String path;

    private static final Logger log = LoggerFactory.getLogger(ProductoController.class);

    public ProductoController(ProductoService service) {
        this.service = service;
    }

    @ModelAttribute("categorias")
    public Flux<Categoria> categorias(){
        return service.findAllCategoria();
    }

    @GetMapping("/uploads/img/{nombreFoto:.+}")
    public Mono<ResponseEntity<Resource>> verFoto(@PathVariable String nombreFoto) throws MalformedURLException {
        Path ruta = Paths.get(path).resolve(nombreFoto).toAbsolutePath();
        Resource imagen = new UrlResource(ruta.toUri());

        return Mono.just(
                ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=\"" + imagen.getFilename() + "\"")
                        .body(imagen)
        );
    }

    @GetMapping("/ver/{id}")
    public Mono<String> ver(Model model, @PathVariable String id){
        return service.findById(id)
                .doOnNext(p -> {
                    model.addAttribute("producto", p);
                    model.addAttribute("titulo", "Detalle producto");
                }).switchIfEmpty(Mono.just(new Producto()))
                .flatMap(producto -> {
                    if (producto.getId() == null){
                        return Mono.error(new InterruptedException("No existe el producto"));
                    }
                    return Mono.just(producto);
                }).then(Mono.just("ver"))
                .onErrorResume(ex -> Mono.just("redirect:/listar?error=no+existe+el+producto"));
    }

    @GetMapping({"/listar", "/"})
    public Mono<String> listar(Model model){
        Flux<Producto> productos = service.findAllWithNameToUpperCase();
        productos.subscribe(producto -> log.info(producto.getNombre() + " " + producto.getCategoria().getId()));

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
        model.addAttribute("boton", "Editar");
        model.addAttribute("producto", productoMono);

        return Mono.just("form");
    }

    @PostMapping("/form")
    public Mono<String> guardar(@Valid Producto producto, BindingResult result, Model model, @RequestPart FilePart file, SessionStatus status){

        if(result.hasErrors()){

            model.addAttribute("titulo", "Errores en formulario producto");
            model.addAttribute("boton", "Guardar");
            return Mono.just("form");
        }else {
            status.setComplete();

            Mono<Categoria> categoriaMono = service.findCategoriaById(producto.getCategoria().getId());

            return categoriaMono.flatMap(categoria -> {

                if (producto.getCreateAt() == null){
                    producto.setCreateAt(new Date());
                }

                if (!file.filename().isEmpty()){
                    producto.setFoto(UUID.randomUUID().toString() + "-" + file.filename()
                            .replace(" ", "")
                            .replace(":", "")
                            .replace("\\", "")
                    );
                }

                producto.setCategoria(categoria);
                return service.save(producto);
            }).doOnNext(p -> {
                log.info("Categoria asignada: " + p.getCategoria().getNombre() + " Id cat: " + p.getCategoria().getId());
                log.info("Producto guardado: " + p.getNombre() + " Id prod: " + p.getId());
            }).flatMap(p -> {
                if(!file.filename().isEmpty()){
                    return file.transferTo(new File(path + p.getFoto()));
                }
                return Mono.empty();
            }).thenReturn("redirect:/listar?success=producto+guardado+con+exito");
        }
    }

    @GetMapping("/eliminar/{id}")
    public Mono<String> eliminar(@PathVariable String id){
        return service.findById(id)
                .defaultIfEmpty(new Producto())
                .flatMap(p -> {
                    if (p.getId() == null){
                        return Mono.error(new InterruptedException("No existe el producto a eliminar"));
                    }
                    return Mono.just(p);
                })
                .flatMap(p -> {
                    log.info("Eliminando producto: " + p.getNombre());
                    log.info("Eliminando producto id: " + p.getId());
                    return service.delete(p);
        })
                .then(Mono.just("redirect:/listar?success=producto+eliminado+con+exito"))
                .onErrorResume(ex -> Mono.just("redirect:/listar?error=no+existe+el+producto+a+eliminar"));
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
