/**
 * EDI/AS2 集成 API
 */
import request from './request'
import type { PaginatedResult } from './types'

export interface EdiDocument {
  id: number
  documentNo: string
  cono: string
  partnerId: number
  partnerCode: string
  partnerName: string
  documentType: string
  direction: string
  status: string
  rawContent: string
  parsedData: string
  errorMessage: string
  processedTime: string
  createdBy: string
  createdTime: string
}

export interface EdiPartner {
  id: number
  partnerCode: string
  partnerName: string
  partnerType: string
  ediVersion: string
  as2Url: string
  as2FromId: string
  as2ToId: string
  as2Micalg: string
  as2Encalgo: string
  as2CertPath: string
  status: string
  createdBy: string
  createdTime: string
}

export const searchEdiDocuments = (params: Record<string, unknown>): Promise<PaginatedResult<EdiDocument>> =>
  request.get('/edi/documents', { params })

export const getEdiDocumentById = (id: number): Promise<EdiDocument> =>
  request.get(`/edi/documents/${id}`)

export const searchEdiPartners = (params: Record<string, unknown>): Promise<PaginatedResult<EdiPartner>> =>
  request.get('/edi/partners', { params })

export const getEdiPartnerById = (id: number): Promise<EdiPartner> =>
  request.get(`/edi/partners/${id}`)

export const getEdiPartnerByCode = (partnerCode: string): Promise<EdiPartner> =>
  request.get('/edi/partners/by-code', { params: { partnerCode } })
