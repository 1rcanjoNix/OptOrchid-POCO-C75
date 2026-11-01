# OptOrchid

Aplicativo Android de monitoramento, diagnóstico e otimização de dispositivos.
Regra principal: **funcionalidade real acima de aparência**. Nada é simulado.

## Estado atual: Fase 1 (Fundação)

Implementado:
- Projeto Kotlin + Jetpack Compose + Material 3 personalizado (preto e violeta).
- Logotipo da orquídea desenhado em código (`OrchidLogo`) e ícone adaptativo com a mesma geometria.
- Inicialização animada: cada mensagem corresponde a uma etapa real (preferências, identificação do aparelho, verificação de fontes de dados).
- Navegação (splash, dashboard, configurações) com transições.
- Dashboard com leituras reais e pontuais: aparelho, RAM, armazenamento, Hz da tela, bateria, alertas básicos.
- Configuração real: reduzir animações (DataStore).

Ainda não implementado (e por isso sem botões falsos nem números inventados):
- Uso de CPU em tempo real, gráficos e histórico (Fase 2).
- Nota Device Performance e botão OTIMIZAR AGORA / Orchid Engine (Fase 4).
- Efeitos sonoros (Fase 5).

## Compilar

Requisitos: Android Studio recente, JDK 17, Android SDK 35.

1. Abra a pasta no Android Studio e aguarde o Gradle Sync (o Studio baixa o Gradle 8.9).
2. Execute o módulo `app` em um aparelho ou emulador (minSdk 26).
3. Pela linha de comando, gere antes o wrapper: `gradle wrapper --gradle-version 8.9` e use `./gradlew assembleDebug`.

## Arquitetura

MVVM com camadas leves, módulo único, injeção manual (`AppContainer`).

```
com.optorchid
├── core/          di · theme · format
├── domain/        model · repository (interfaces) · usecase
├── data/          device (APIs do Android) · preferences (DataStore)
├── presentation/  components · splash · dashboard · settings
└── navigation/
```

## Permissões

Nenhuma na Fase 1.

## Gerar o APK pelo GitHub

O workflow `.github/workflows/build-apk.yml` compila o APK de depuração a cada push na branch `main`
(ou manualmente em Actions > Build APK > Run workflow). O arquivo `.apk` fica em
**Artifacts** na página da execução. Se a compilação falhar, o log da etapa "Compilar APK de depuração"
mostra o erro.
