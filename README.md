# SemaFlux

SemaFlux é um simulador de semáforos e fluxo de tráfego urbano desenvolvido em Java utilizando JavaFX. O sistema permite simular o tráfego urbano com diferentes estratégias de controle de semáforos, visualizando o comportamento do fluxo de veículos em tempo real.

<p align="center">
  <img src="SemaFlux.png" alt="SemaFlux Logo" width="300">
</p>

## Funcionalidades

- Simulação de tráfego urbano com diferentes estratégias de controle de semáforos
- Visualização em tempo real do fluxo de veículos
- Três modos de operação de semáforos:
  - **Tempo Fixo**: Ciclos de tempos constantes
  - **Adaptativo**: Semáforos ajustam tempos com base no volume de tráfego
  - **Economia de Energia**: Otimiza o consumo em períodos de baixo fluxo
- Importação de mapas personalizados em formato JSON
- Interface gráfica para configuração de parâmetros da simulação

## Requisitos de Sistema

- Java 11 ou superior
- Maven 3.6 ou superior
- Espaço em disco: aproximadamente 100MB

## Instalação e Execução

### Clonando o Repositório

```bash
git clone https://github.com/WarneyRego/Semaflux--final
cd semaflux
```

### Compilando o Projeto

O projeto utiliza Maven para gerenciamento de dependências e build. Para compilar:

```bash
mvn clean compile
```

### Executando a Aplicação

Para iniciar o SemaFlux:

```bash
mvn javafx:run
```

Alternativamente, após compilar o projeto, você pode gerar um arquivo JAR executável:

```bash
mvn package
java -jar target/Semaflux-1.0.jar
```

## Configurando a Simulação

Ao iniciar o SemaFlux, você verá a tela de configuração com as seguintes opções:

1. **Seleção de Mapa**:
   - Escolha o mapa pré-definido (Jóquei - Teresina, Piauí) ou
   - Selecione "Personalizado" para importar seu próprio arquivo JSON

2. **Configuração Geral**:
   - Modo de Semáforo: Fixo, Adaptativo ou Economia de Energia
   - Taxa de Geração de Veículos: controla o volume de tráfego
   - Horário de Pico: ativa condições de tráfego intenso
   - Duração da Simulação: tempo total da simulação (em segundos)

3. **Parâmetros Específicos do Modo**:
   - Cada modo de semáforo possui parâmetros configuráveis específicos
   - Ajuste tempos de verde, amarelo e vermelho
   - Configure limiares e incrementos para modos adaptativos

## Importando Mapas Personalizados

Para utilizar mapas personalizados:

1. Selecione "Personalizado" no seletor de mapas
2. Clique no botão "Procurar" para abrir o seletor de arquivos
3. Navegue até seu arquivo JSON
4. Clique em "Abrir"

### Formato do Arquivo JSON

Os arquivos JSON para mapas personalizados devem seguir esta estrutura:

```json
{
  "nodes": [
    {
      "id": "node1",
      "latitude": 5.1234,
      "longitude": -42.5678
    },
    ...
  ],
  "edges": [
    {
      "id": "edge1",
      "source": "node1",
      "target": "node2",
      "oneway": true,
      "maxspeed": 40,
      "length": 100
    },
    ...
  ],
  "traffic_lights": [
    {
      "id": "node1",
      "attributes": {
        "traffic_signals:direction": "ns-ew"
      }
    },
    ...
  ]
}
```

## Controles da Simulação

Durante a simulação:

- **Zoom**: Use a roda do mouse para aumentar/diminuir o zoom
- **Arrastar**: Clique e arraste o mapa para navegar
- **Pausa**: Botão no canto inferior direito para pausar/continuar a simulação
- **Estatísticas**: Painel lateral mostra informações em tempo real

## Resolução de Problemas

### Erro de Java ou JavaFX

Se encontrar erros relacionados ao JavaFX:

```bash
mvn javafx:run -f pom.xml
```

### Erro de Carregamento de Mapa

Verifique se o arquivo JSON segue o formato correto e contém todos os campos necessários.

### Desempenho Lento

Para simulações grandes, considere:
- Reduzir a taxa de geração de veículos
- Utilizar um mapa menor
- Aumentar a memória disponível para a JVM:
```bash
mvn javafx:run -Djavafx.run.jvmArgs="-Xmx2g"
```

 