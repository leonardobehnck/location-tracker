# Location Tracker

App de rastreamento de localização em tempo (quase) real para Android e iOS, feito com **Compose Multiplatform**.

## O que o app faz

- **Captura a localização do dispositivo** em intervalos regulares (a cada 30 segundos no Android).
- **Envia as posições para uma API.**
- **Funciona em segundo plano** no Android via serviço em foreground (com notificação).
- **Suporta uso offline**: localizações são guardadas em cache e enviadas quando a rede voltar (WorkManager).
- Na tela, o usuário pode **solicitar permissão de localização**, **iniciar** e **parar** o rastreamento.

## Estrutura do projeto

- **[composeApp](./composeApp/src)** – código compartilhado e específico por plataforma:
  - **commonMain** – código comum (expect/actual, App, MainScreen).
  - **androidMain** – Android: serviço de localização, WorkManager, cache, API, permissões, UI Compose.
  - **iosMain** – iOS: inicialização e UI Compose.
- **[iosApp](./iosApp)** – app iOS (entrada Swift/Xcode) que usa o framework gerado pelo Compose.

No Android, a arquitetura inclui:

- **LocationTrackingService** – serviço em foreground que obtém localização (Fused Location Provider) e envia para a API.
- **LocationSyncWorker** – worker que sincroniza localizações pendentes quando há rede.
- **LocationTrackingRepository** – orquestra captura, cache e envio.
- **Api** – contrato e implementação HTTP para enviar os dados de localização.

## Pré-requisitos

- JDK 17+
- Android Studio (para Android) e/ou Xcode (para iOS)
- Android: permissões de localização; iOS: configuração de background e permissões conforme o projeto

## Build e execução

### Android

```bash
./gradlew :composeApp:assembleDebug
```

Ou use a run configuration do Android Studio. O app pede permissão de localização e permite iniciar/parar o rastreamento.

### iOS

Abra a pasta [iosApp](./iosApp) no Xcode e rode o app no simulador ou dispositivo.

## Tecnologias

- Kotlin Multiplatform (KMP)
- Compose Multiplatform (UI)
- Koin (injeção de dependência)
- Android: Fused Location Provider, WorkManager, foreground service
- Envio de localização para API REST

---

Para mais sobre KMP: [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html).
