import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'

const STORAGE_KEY = 'meditrack_auth'

function resolveAuthToken(): string | null {
  try {
    const stored = localStorage.getItem(STORAGE_KEY)
    if (stored) {
      const parsed = JSON.parse(stored) as { token?: string }
      if (parsed?.token) {
        return parsed.token
      }
    }

    return localStorage.getItem('token') ?? localStorage.getItem('sessionToken') ?? null
  } catch {
    return null
  }
}

export class WebSocketService {
  private static instance: WebSocketService
  private stompClient: Client | null = null
  private connected = false
  private subscriptions: Map<string, any> = new Map()

  private constructor() {}

  static getInstance(): WebSocketService {
    if (!WebSocketService.instance) {
      WebSocketService.instance = new WebSocketService()
    }
    return WebSocketService.instance
  }

  connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      if (this.connected) {
        resolve()
        return
      }

      const token = resolveAuthToken()
      if (!token) {
        reject(new Error('Authentication token required for WebSocket connection'))
        return
      }

      // Primary: notification service (port 8086), Fallback: alert service (port 8085)
      const baseUrl = import.meta.env.VITE_DASHBOARD_WS_URL ?? 'http://localhost:8086/ws-alerts'
      const socketUrl = `${baseUrl}${baseUrl.includes('?') ? '&' : '?'}token=${encodeURIComponent(token)}`

      this.stompClient = new Client({
        webSocketFactory: () => new SockJS(socketUrl),
        debug: (str) => {
          if (str.includes('ERROR') || str.includes('Connection')) {
            console.log('STOMP Debug:', str)
          }
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
      })

      this.stompClient.onConnect = (frame) => {
        console.log('Connected to WebSocket:', frame)
        this.connected = true
        resolve()
      }

      this.stompClient.onStompError = (frame) => {
        console.error('STOMP Error:', frame)
        this.connected = false
        reject(new Error('WebSocket connection failed'))
      }

      this.stompClient.onDisconnect = () => {
        console.log('WebSocket disconnected')
        this.connected = false
      }

      this.stompClient.activate()
    })
  }

  disconnect() {
    if (this.stompClient) {
      this.subscriptions.forEach((subscription) => {
        subscription.unsubscribe()
      })
      this.subscriptions.clear()

      this.stompClient.deactivate()
      this.stompClient = null
      this.connected = false
    }
  }

  subscribe(destination: string, callback: (message: any) => void): string {
    if (!this.connected || !this.stompClient) {
      console.error('WebSocket not connected')
      return ''
    }

    const subscription = this.stompClient.subscribe(destination, (message) => {
      try {
        const data = JSON.parse(message.body)
        callback(data)
      } catch (error) {
        console.error('Error parsing WebSocket message:', error)
      }
    })

    this.subscriptions.set(destination, subscription)
    return subscription.id
  }

  unsubscribe(destination: string) {
    const subscription = this.subscriptions.get(destination)
    if (subscription) {
      subscription.unsubscribe()
      this.subscriptions.delete(destination)
    }
  }

  isConnected(): boolean {
    return this.connected
  }

  private departmentTopicSegment(department: string): string {
    return department.trim().toLowerCase().replace(/[^a-z0-9_-]/g, '-')
  }

  subscribeToDoctorVitals(department: string, callback: (data: any) => void): string {
    return this.subscribe(`/topic/doctor-vitals/department/${this.departmentTopicSegment(department)}`, callback)
  }

  subscribeToDoctorPredictions(department: string, callback: (data: any) => void): string {
    return this.subscribe(`/topic/doctor-predictions/department/${this.departmentTopicSegment(department)}`, callback)
  }

  subscribeToDoctorAlerts(department: string, callback: (data: any) => void): string {
    return this.subscribe(`/topic/doctor-alerts/department/${this.departmentTopicSegment(department)}`, callback)
  }

  subscribeToNurseVitals(department: string, callback: (data: any) => void): string {
    return this.subscribe(`/topic/nurse-vitals/department/${this.departmentTopicSegment(department)}`, callback)
  }

  subscribeToNurseAlerts(department: string, callback: (data: any) => void): string {
    return this.subscribe(`/topic/nurse-alerts/department/${this.departmentTopicSegment(department)}`, callback)
  }

  subscribeToAdminVitals(callback: (data: any) => void): string {
    return this.subscribe('/topic/admin-vitals', callback)
  }

  subscribeToAdminPredictions(callback: (data: any) => void): string {
    return this.subscribe('/topic/admin-predictions', callback)
  }

  subscribeToAdminAlerts(callback: (data: any) => void): string {
    return this.subscribe('/topic/admin-alerts', callback)
  }

  subscribeToDepartmentAlerts(department: string, callback: (data: any) => void): string {
    return this.subscribe(`/topic/alerts/department/${this.departmentTopicSegment(department)}`, callback)
  }
}

export default WebSocketService
