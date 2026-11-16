
package pt.utp.estg.pp.printhub.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.utp.estg.pp.printhub.entity.Color;
import pt.utp.estg.pp.printhub.repository.ColorRepository;

import java.math.BigDecimal;
import java.util.NoSuchElementException;

@Service
public class StockService {

    private final ColorRepository colorRepository;

    public StockService(ColorRepository colorRepository) {
        this.colorRepository = colorRepository;
    }

    // Consultar stock de uma combinação material/cor
    @Transactional(readOnly = true)
    public BigDecimal getAvailableStock(Integer materialId, Integer colorId) {
        Color color = findValidColor(materialId, colorId);
        return color.getStockGrams();
    }

    // Verificar se existe stock suficiente sem descontar
    @Transactional(readOnly = true)
    public void validateStock(
            Integer materialId,
            Integer colorId,
            BigDecimal requiredGrams) {

        validateQuantity(requiredGrams);

        Color color = findValidColor(materialId, colorId);

        if (color.getStockGrams().compareTo(requiredGrams) < 0) {
            throw new IllegalArgumentException(
                    "Stock insuficiente. Disponível: "
                            + color.getStockGrams()
                            + " g. Necessário: "
                            + requiredGrams + " g."
            );
        }
    }

    // Descontar stock com bloqueio para evitar pedidos simultâneos
    @Transactional
    public void deductStock(
            Integer materialId,
            Integer colorId,
            BigDecimal requiredGrams) {

        validateQuantity(requiredGrams);

        Color color = colorRepository.findByIdForUpdate(colorId)
                .orElseThrow(() ->
                        new NoSuchElementException("Cor não encontrada."));

        validateColorMaterial(color, materialId);

        if (color.getStockGrams().compareTo(requiredGrams) < 0) {
            throw new IllegalArgumentException(
                    "Stock insuficiente para realizar o pedido."
            );
        }

        color.setStockGrams(
                color.getStockGrams().subtract(requiredGrams)
        );

        colorRepository.save(color);
    }


    @Transactional
    public void restoreStock(
            Integer materialId,
            Integer colorId,
            BigDecimal gramsToRestore) {

        validateQuantity(gramsToRestore);

        // Bloquear a cor durante a reposição de stock
        Color color = colorRepository.findByIdForUpdate(colorId)
                .orElseThrow(() ->
                        new NoSuchElementException("Cor não encontrada."));

        // Confirmar que a cor pertence ao material indicado
        if (materialId == null ||
                !color.getMaterial().getId().equals(materialId)) {
            throw new IllegalArgumentException(
                    "A cor selecionada não pertence ao material indicado."
            );
        }

        // Repor o stock anteriormente reservado
        color.setStockGrams(
                color.getStockGrams().add(gramsToRestore)
        );

        colorRepository.save(color);
    }
    // Confirmar que a cor existe e pertence ao material
    private Color findValidColor(Integer materialId, Integer colorId) {
        Color color = colorRepository.findById(colorId)
                .orElseThrow(() ->
                        new NoSuchElementException("Cor não encontrada."));

        validateColorMaterial(color, materialId);
        return color;
    }

    private void validateColorMaterial(Color color, Integer materialId) {
        if (materialId == null
                || !color.getMaterial().getId().equals(materialId)) {
            throw new IllegalArgumentException(
                    "A cor selecionada não pertence ao material indicado."
            );
        }

        if (!Boolean.TRUE.equals(color.getAvailable())
                || !Boolean.TRUE.equals(color.getMaterial().getActive())) {
            throw new IllegalArgumentException(
                    "O material ou a cor não está disponível."
            );
        }
    }

    private void validateQuantity(BigDecimal requiredGrams) {
        if (requiredGrams == null
                || requiredGrams.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "A quantidade de material deve ser superior a zero."
            );
        }
    }

    @Transactional
    public BigDecimal addStock(
            Integer materialId,
            Integer colorId,
            BigDecimal gramsToAdd) {

        validateQuantity(gramsToAdd);

        Color color = colorRepository.findByIdForUpdate(colorId)
                .orElseThrow(() ->
                        new NoSuchElementException("Cor não encontrada."));

        if (materialId == null ||
                !color.getMaterial().getId().equals(materialId)) {
            throw new IllegalArgumentException(
                    "A cor não pertence ao material indicado."
            );
        }

        BigDecimal newStock = color.getStockGrams().add(gramsToAdd);

        if (newStock.precision() - newStock.scale() > 10 ||
                newStock.scale() > 2) {
            throw new IllegalArgumentException(
                    "Quantidade de stock fora dos limites permitidos."
            );
        }

        color.setStockGrams(newStock);

        colorRepository.save(color);

        return newStock;
    }

}
