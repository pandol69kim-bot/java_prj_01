import apiClient from './client'

export interface ExternalSystem {
  id: number
  systemCode: string
  systemName: string
  baseUrl: string
  authType: 'NONE' | 'API_KEY' | 'BEARER' | 'BASIC'
  active: boolean
  createdAt: string
  updatedAt: string
}

export interface ExternalSystemPage {
  content: ExternalSystem[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

export interface CreateExternalSystemRequest {
  systemCode: string
  systemName: string
  baseUrl: string
  authType: 'NONE' | 'API_KEY' | 'BEARER' | 'BASIC'
  apiKey?: string
}

export interface UpdateApiKeyRequest {
  apiKey: string
}

export const externalSystemApi = {
  getList(page = 0, size = 20) {
    return apiClient.get<{ data: ExternalSystemPage }>('/external-systems', {
      params: { page, size },
    })
  },

  getActive() {
    return apiClient.get<{ data: ExternalSystem[] }>('/external-systems/active')
  },

  getById(id: number) {
    return apiClient.get<{ data: ExternalSystem }>(`/external-systems/${id}`)
  },

  create(payload: CreateExternalSystemRequest) {
    return apiClient.post<{ data: ExternalSystem }>('/external-systems', payload)
  },

  updateApiKey(id: number, payload: UpdateApiKeyRequest) {
    return apiClient.patch<{ data: ExternalSystem }>(`/external-systems/${id}/api-key`, payload)
  },

  deactivate(id: number) {
    return apiClient.delete<{ data: ExternalSystem }>(`/external-systems/${id}`)
  },
}
