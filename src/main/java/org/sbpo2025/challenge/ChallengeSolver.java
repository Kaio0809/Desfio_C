package org.sbpo2025.challenge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang3.time.StopWatch;

public class ChallengeSolver {
    private final long MAX_RUNTIME = 600000; // milliseconds; 10 minutes
    
    protected List<Map<Integer, Integer>> orders;
    protected List<Map<Integer, Integer>> aisles;
    protected int nItems;
    protected int waveSizeLB;
    protected int waveSizeUB;
    
    public ChallengeSolver(
        List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems, int waveSizeLB, int waveSizeUB) {
            this.orders = orders;
            this.aisles = aisles;
            this.nItems = nItems;
            this.waveSizeLB = waveSizeLB;
            this.waveSizeUB = waveSizeUB;
    }
    
    // Método auxiliar para calcular eficiência
    private double calcularEficiencia(PedidoComIndice pedido, List<Map<Integer, Integer>> corredores) {
        Set<Integer> corredoresNecessarios = new HashSet<>();
        for (int item : pedido.itens.keySet()) {
            for (int i = 0; i < corredores.size(); i++) {
                if (corredores.get(i).containsKey(item)) {
                    corredoresNecessarios.add(i);
                }
            }
        }
        return pedido.totalItens / (double) (corredoresNecessarios.size() + 1e-6);
    }
    
    class PedidoComIndice {
        int indice;
        Map<Integer, Integer> itens;
        int totalItens;
        
        PedidoComIndice(int indice, Map<Integer, Integer> itens) {
            this.indice = indice;
            this.itens = itens;
            this.totalItens = itens.values().stream().mapToInt(Integer::intValue).sum();
        }
    }
    public ChallengeSolution solve(StopWatch stopWatch) {
        // 1. Criar lista de pedidos com índice
        List<PedidoComIndice> pedidos = new ArrayList<>();
        for (int i = 0; i < orders.size(); i++) {
            pedidos.add(new PedidoComIndice(i, orders.get(i)));
        }
        
        // 2. Ordenar pedidos por eficiência (itens / corredores necessários)
        Comparator<PedidoComIndice> efComparator = (p1, p2) -> {
            double ef1 = calcularEficiencia(p1, aisles);
            double ef2 = calcularEficiencia(p2, aisles);
            return Double.compare(ef2, ef1); // ordem decrescente
        };
        pedidos.sort(efComparator);
        
        List<PedidoComIndice> pedidosSelecionados = new ArrayList<>();
        Set<Integer> corredoresSelecionados = new LinkedHashSet<>();
        Map<Integer, Integer> totalPorItem = new HashMap<>();
        int totalItensSelecionados = 0;
        
        // 3. Seleção gulosa de pedidos
        for (PedidoComIndice pedido : pedidos) {
            // Encontrar corredores que têm os itens deste pedido
            Set<Integer> corredoresNecessarios = new HashSet<>();
            for (int item : pedido.itens.keySet()) {
                for (int i = 0; i < aisles.size(); i++) {
                    if (aisles.get(i).containsKey(item)) {
                        corredoresNecessarios.add(i);
                    }
                }
            }
            
            // Verificar capacidade disponível para este pedido
            boolean capacidade = true;
            for (Map.Entry<Integer, Integer> entry : pedido.itens.entrySet()) {
                int item = entry.getKey();
                int quantidade = entry.getValue();
                
                int disponivelNosCorredores = 0;
                for (int corredor : corredoresNecessarios) {
                    disponivelNosCorredores += aisles.get(corredor).getOrDefault(item, 0);
                }
                
                int jaSelecionado = totalPorItem.getOrDefault(item, 0);
                if (disponivelNosCorredores < quantidade + jaSelecionado) {
                    capacidade = false;
                    break;
                }
            }
            
            int novoTotal = totalItensSelecionados + pedido.totalItens;
            if (capacidade && novoTotal <= waveSizeUB) {
                pedidosSelecionados.add(pedido);
                corredoresSelecionados.addAll(corredoresNecessarios);
                totalItensSelecionados = novoTotal;
                
                for (Map.Entry<Integer, Integer> entry : pedido.itens.entrySet()) {
                    totalPorItem.put(entry.getKey(),
                            totalPorItem.getOrDefault(entry.getKey(), 0) + entry.getValue());
                }
                
                if (totalItensSelecionados >= waveSizeLB) {
                    break;
                }
            }
        }
        
        // 4. Minimizar corredores redundantes garantindo cobertura quantitativa
        Set<Integer> corredoresOtimizados = new LinkedHashSet<>();
        
        // Mapa para acompanhar a cobertura atual (quantidade disponível somada) de cada item
        Map<Integer, Integer> coberturaAtual = new HashMap<>();
        for (Integer item : totalPorItem.keySet()) {
            coberturaAtual.put(item, 0);
        }
        
        while (true) {
            int melhorCorredor = -1;
            int melhorIncremento = 0;
        
            for (int i = 0; i < aisles.size(); i++) {
                if (corredoresOtimizados.contains(i)) continue;
        
                int incremento = 0;
                Map<Integer, Integer> corredorItens = aisles.get(i);
        
                for (Map.Entry<Integer, Integer> entry : corredorItens.entrySet()) {
                    int item = entry.getKey();
                    int disponivel = entry.getValue();
        
                    if (!totalPorItem.containsKey(item)) continue;
        
                    int faltante = totalPorItem.get(item) - coberturaAtual.get(item);
                    if (faltante > 0) {
                        incremento += Math.min(faltante, disponivel);
                    }
                }
        
                if (incremento > melhorIncremento) {
                    melhorIncremento = incremento;
                    melhorCorredor = i;
                }
            }
        
            if (melhorCorredor == -1 || melhorIncremento == 0) {
                // Nenhum corredor melhora a cobertura, termina a seleção
                break;
            }
        
            corredoresOtimizados.add(melhorCorredor);
        
            // Atualiza cobertura atual dos itens com o corredor selecionado
            Map<Integer, Integer> corredorSelecionado = aisles.get(melhorCorredor);
            for (Map.Entry<Integer, Integer> entry : corredorSelecionado.entrySet()) {
                int item = entry.getKey();
                if (!totalPorItem.containsKey(item)) continue;
        
                int atual = coberturaAtual.get(item);
                int disponivel = entry.getValue();
                coberturaAtual.put(item, Math.min(totalPorItem.get(item), atual + disponivel));
            }
        
            // Verifica se todos os itens já estão totalmente cobertos
            boolean tudoCoberto = true;
            for (Integer item : totalPorItem.keySet()) {
                if (coberturaAtual.get(item) < totalPorItem.get(item)) {
                    tudoCoberto = false;
                    break;
                }
            }
            if (tudoCoberto) break;
        }
        
        System.out.println("Pedidos finais selecionados: " +
                pedidosSelecionados.stream().map(p -> p.indice).collect(Collectors.toSet()));
        System.out.println("Corredores finais selecionados: " + corredoresOtimizados);
        
        Set<Integer> pedidosIndices = pedidosSelecionados.stream()
                .map(p -> p.indice)
                .collect(Collectors.toSet());
        
        return new ChallengeSolution(pedidosIndices, corredoresOtimizados);
    }
    
    /* O resto do código permanece o mesmo */
    
    protected long getRemainingTime(StopWatch stopWatch) {
        return Math.max(
                TimeUnit.SECONDS.convert(MAX_RUNTIME - stopWatch.getTime(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS),
                0);
    }
    
    protected boolean isSolutionFeasible(ChallengeSolution challengeSolution) {
        Set<Integer> selectedOrders = challengeSolution.orders();
        Set<Integer> visitedAisles = challengeSolution.aisles();
        if (selectedOrders == null || visitedAisles == null || selectedOrders.isEmpty() || visitedAisles.isEmpty()) {
            return false;
        }
        
        int[] totalUnitsPicked = new int[nItems];
        int[] totalUnitsAvailable = new int[nItems];
        
        // Calculate total units picked
        for (int order : selectedOrders) {
            for (Map.Entry<Integer, Integer> entry : orders.get(order).entrySet()) {
                totalUnitsPicked[entry.getKey()] += entry.getValue();
            }
        }
        
        // Calculate total units available
        for (int aisle : visitedAisles) {
            for (Map.Entry<Integer, Integer> entry : aisles.get(aisle).entrySet()) {
                totalUnitsAvailable[entry.getKey()] += entry.getValue();
            }
        }
        
        // Check if the total units picked are within bounds
        int totalUnits = Arrays.stream(totalUnitsPicked).sum();
        if (totalUnits < waveSizeLB || totalUnits > waveSizeUB) {
            return false;
        }
        
        // Check if the units picked do not exceed the units available
        for (int i = 0; i < nItems; i++) {
            if (totalUnitsPicked[i] > totalUnitsAvailable[i]) {
                return false;
            }
        }
        
        return true;
    }
    
    protected double computeObjectiveFunction(ChallengeSolution challengeSolution) {
        Set<Integer> selectedOrders = challengeSolution.orders();
        Set<Integer> visitedAisles = challengeSolution.aisles();
        if (selectedOrders == null || visitedAisles == null || selectedOrders.isEmpty() || visitedAisles.isEmpty()) {
            return 0.0;
        }
        int totalUnitsPicked = 0;
        
        // Calculate total units picked
        for (int order : selectedOrders) {
            totalUnitsPicked += orders.get(order).values().stream()
                    .mapToInt(Integer::intValue)
                    .sum();
        }
        
        // Calculate the number of visited aisles
        int numVisitedAisles = visitedAisles.size();
        
        // Objective function: total units picked / number of visited aisles
        return (double) totalUnitsPicked / numVisitedAisles;
    }
}
