package pt.utp.estg.pp.printhub.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pt.utp.estg.pp.printhub.dto.CategoryDTO;
import pt.utp.estg.pp.printhub.entity.Category;
import pt.utp.estg.pp.printhub.repository.CategoryRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(categoryRepository);
    }

    @Test
    void deveCriarCategoria() {
        when(categoryRepository.existsByNameIgnoreCase("Decoracao"))
                .thenReturn(false);

        when(categoryRepository.save(any(Category.class)))
                .thenAnswer(invocation -> {
                    Category category = invocation.getArgument(0);
                    category.setId(1);
                    return category;
                });

        CategoryDTO result = categoryService.createCategory(" Decoracao ");

        assertEquals(1, result.id());
        assertEquals("Decoracao", result.name());
        assertTrue(result.active());

        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void naoDeveCriarCategoriaSemNome() {
        assertThrows(
                IllegalArgumentException.class,
                () -> categoryService.createCategory(" ")
        );

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void naoDeveCriarCategoriaDuplicada() {
        when(categoryRepository.existsByNameIgnoreCase("Decoracao"))
                .thenReturn(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> categoryService.createCategory("Decoracao")
        );

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void deveListarCategorias() {
        Category category = new Category();
        category.setId(1);
        category.setName("Decoracao");
        category.setActive(true);

        when(categoryRepository.findAll())
                .thenReturn(List.of(category));

        List<CategoryDTO> result = categoryService.listCategories();

        assertEquals(1, result.size());
        assertEquals("Decoracao", result.get(0).name());
    }

    @Test
    void deveAtualizarCategoria() {
        Category category = new Category();
        category.setId(1);
        category.setName("Antigo");
        category.setActive(true);

        when(categoryRepository.findById(1))
                .thenReturn(Optional.of(category));

        when(categoryRepository.save(any(Category.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CategoryDTO dto = new CategoryDTO(1, "Novo", true);

        CategoryDTO result = categoryService.updateCategory(1, dto);

        assertEquals("Novo", result.name());
        assertTrue(result.active());

        verify(categoryRepository).save(category);
    }

    @Test
    void naoDeveAtualizarCategoriaInexistente() {
        when(categoryRepository.findById(99))
                .thenReturn(Optional.empty());

        CategoryDTO dto = new CategoryDTO(99, "Nova", true);

        assertThrows(
                NoSuchElementException.class,
                () -> categoryService.updateCategory(99, dto)
        );
    }

    @Test
    void deveDesativarCategoria() {
        Category category = new Category();
        category.setId(1);
        category.setName("Decoracao");
        category.setActive(true);

        when(categoryRepository.findById(1))
                .thenReturn(Optional.of(category));

        categoryService.deleteCategory(1);

        assertFalse(category.getActive());

        verify(categoryRepository).save(category);
        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    void naoDeveDesativarCategoriaInexistente() {
        when(categoryRepository.findById(99))
                .thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> categoryService.deleteCategory(99)
        );
    }
}
