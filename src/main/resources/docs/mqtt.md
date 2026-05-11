# Connect SDRTrunk Kennebec to an MQTT Broker

SDRTrunk Kennebec can publish radio events to an MQTT broker, letting you integrate scanner activity with home automation systems, notification platforms, dashboards, or any tool that speaks MQTT. When MQTT is enabled, the application connects to the broker you specify and begins publishing events as they occur in real time. Common use cases include triggering home automation routines on specific talkgroup activity, feeding Two Tone Detect events to a paging integration, and building custom monitoring displays.

## Open the MQTT preferences panel

  **1. Open User Preferences**

    In the main application window, open the **View** menu and select **User Preferences**.

  **2. Navigate to MQTT**

    In the left sidebar, under the **Application** section header, click **MQTT**.


---

## Configuration fields

### Enable MQTT

The **Enable MQTT** checkbox activates or deactivates the integration. When unchecked, all other fields are disabled and the application makes no MQTT connections. The integration is **disabled by default**. Enable it only after you have configured at least a valid server URL.

### Server / Host

The full URL of your MQTT broker, including the protocol scheme and port. Default: `tcp://localhost:1883`.

| Scenario | URL format |
| --- | --- |
| Local broker (unencrypted) | `tcp://localhost:1883` |
| Remote broker (unencrypted) | `tcp://192.168.1.50:1883` |
| Remote broker (TLS) | `ssl://broker.example.com:8883` |

### Username and password

Enter the credentials required by your broker. Leave both fields empty if your broker does not require authentication. The password field masks input for security.

> **Warning**
>
  SDRTrunk Kennebec stores the MQTT password in plaintext within its XML preferences file. Do not use a sensitive or shared password. Use a dedicated broker account with minimal permissions.

### Client ID

The MQTT client identifier sent to the broker when the connection is established. Broker logs and dashboards display this value to identify the SDRTrunk session.

Leave this field blank and the application automatically generates a unique identifier in the format `SDRTrunk-{UUID}` — for example, `SDRTrunk-a3f2c1d0-4e5b-...`. Set a fixed value such as `SDRTrunk-HomeStation` if you want a stable, human-readable identifier in your broker logs.

> **Note**
>
  Most brokers require each connected client to have a unique ID. If two SDRTrunk instances connect with the same client ID, the broker will typically disconnect the earlier session.

---

## Enable MQTT step by step

  **3. Start your MQTT broker**

    Ensure your broker (such as Mosquitto or a cloud service) is running and reachable from the machine running SDRTrunk Kennebec. Confirm you can connect to it with an MQTT client before configuring SDRTrunk.

  **4. Enter the server URL**

    Type the full broker URL into the **Server/Host** field. Use `tcp://` for unencrypted connections or `ssl://` for TLS.

  **5. Add credentials (if required)**

    If your broker requires authentication, enter the **Username** and **Password** for the broker account SDRTrunk should use.

  **6. Set a client ID (optional)**

    Leave the **Client ID** field blank to auto-generate an ID, or enter a fixed value such as `SDRTrunk-HomeStation` for a stable identifier in your broker logs.

  **7. Enable the integration**

    Check **Enable MQTT**. SDRTrunk Kennebec connects to the broker immediately and begins publishing events.


> **Tip**
>
  Use a tool like MQTT Explorer or `mosquitto_sub` to verify that events are arriving at your broker after enabling the integration.

---

## Disabling MQTT

Uncheck **Enable MQTT** at any time to disconnect from the broker. The connection closes immediately and no further events are published. Your server URL and credentials are preserved so you can re-enable the integration without re-entering them.

---

## Use cases

  **Home automation**

    Trigger smart home routines when specific talkgroups are active — for example, turning on a light when fire dispatch traffic is heard.

  **Paging integrations**

    Feed Two Tone Detect events to paging platforms or notification services by publishing tone match results to a dedicated MQTT topic.

  **Custom dashboards**

    Subscribe to the event stream from Node-RED, Grafana, or any MQTT-aware tool to build real-time monitoring displays.

  **Multi-instance coordination**

    Use a shared broker to coordinate activity between multiple SDRTrunk instances running on different machines.
