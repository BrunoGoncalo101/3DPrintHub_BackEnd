
package pt.utp.estg.pp.printhub.service;

import org.springframework.stereotype.Service;
import pt.utp.estg.pp.printhub.dto.ProductDTO;
import pt.utp.estg.pp.printhub.entity.Category;
import pt.utp.estg.pp.printhub.entity.Product;
import pt.utp.estg.pp.printhub.repository.CategoryRepository;
import pt.utp.estg.pp.printhub.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pt.utp.estg.pp.printhub.specification.ProductSpecification;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository) {

        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    // Pesquisar produtos com filtros e paginação
    public Page<ProductDTO> searchProducts(
            String texto,
            Integer categoriaId,
            Integer materialId,
            Integer corId,
            BigDecimal precoMin,
            BigDecimal precoMax,
            Pageable pageable) {

        Page<Product> products = productRepository.findAll(
                ProductSpecification.withFilters(
                        texto,
                        categoriaId,
                        materialId,
                        corId,
                        precoMin,
                        precoMax
                ),
                pageable
        );

        return products.map(this::toDTO);
    }
    // ver o produto pelo ID
    public ProductDTO getProductById(Integer id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Produto não encontrado."
                ));

        return toDTO(product);
    }

    // Criar produto
    public ProductDTO createProduct(ProductDTO dto) {

        validateProduct(dto);

        Category category = findActiveCategory(dto.categoryId());

        Product product = new Product();

        product.setName(dto.name().trim());
        product.setDescription(dto.description());
        product.setBasePrice(dto.basePrice());
        product.setImageUrl(dto.imageUrl());
        product.setDimensions(dto.dimensions());
        product.setActive(true);
        product.setCategory(category);

        Product savedProduct = productRepository.save(product);

        return toDTO(savedProduct);
    }

    // Editar produto
    public ProductDTO updateProduct(Integer id, ProductDTO dto) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Produto não encontrado."
                ));

        validateProduct(dto);

        Category category = findActiveCategory(dto.categoryId());

        product.setName(dto.name().trim());
        product.setDescription(dto.description());
        product.setBasePrice(dto.basePrice());
        product.setImageUrl(dto.imageUrl());
        product.setDimensions(dto.dimensions());
        product.setCategory(category);

        if (dto.active() != null) {
            product.setActive(dto.active());
        }

        Product updatedProduct = productRepository.save(product);

        return toDTO(updatedProduct);
    }

    // Desativar produto
    public void deleteProduct(Integer id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Produto não encontrado."
                ));

        product.setActive(false);

        productRepository.save(product);
    }

    // Validar os dados recebidos
    private void validateProduct(ProductDTO dto) {

        if (dto == null) {
            throw new IllegalArgumentException(
                    "Os dados do produto são obrigatórios."
            );
        }

        if (dto.name() == null || dto.name().isBlank()) {
            throw new IllegalArgumentException(
                    "O nome do produto é obrigatório."
            );
        }

        if (dto.name().trim().length() > 150) {
            throw new IllegalArgumentException(
                    "O nome não pode ultrapassar 150 caracteres."
            );
        }

        if (dto.description() != null
                && dto.description().length() > 500) {
            throw new IllegalArgumentException(
                    "A descrição não pode ultrapassar 500 caracteres."
            );
        }

        if (dto.basePrice() == null
                || dto.basePrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "O preço deve ser superior a zero."
            );
        }

        if (dto.basePrice().scale() > 2
                || dto.basePrice().precision()
                - dto.basePrice().scale() > 8) {
            throw new IllegalArgumentException(
                    "O preço deve respeitar o formato DECIMAL(10,2)."
            );
        }

        if (dto.categoryId() == null) {
            throw new IllegalArgumentException(
                    "A categoria é obrigatória."
            );
        }
    }

    // Procurar categoria válida
    private Category findActiveCategory(Integer categoryId) {

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Categoria não encontrada."
                ));

        if (!Boolean.TRUE.equals(category.getActive())) {
            throw new IllegalArgumentException(
                    "Não é possível associar uma categoria inativa."
            );
        }

        return category;
    }

    // Converter entidade para DTO
    private ProductDTO toDTO(Product product) {

        return new ProductDTO(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getBasePrice(),
                product.getImageUrl(),
                product.getDimensions(),
                product.getActive(),
                product.getCategory().getId()
        );
    }
}
