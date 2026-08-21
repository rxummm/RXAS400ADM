import request from './request'

export const fetchLibraries = () => request.get('/source/libraries')

export const fetchSourceFiles = (library: string) =>
  request.get(`/source/libraries/${library}/files`)

export const fetchMembers = (library: string, sourceFile: string) =>
  request.get(`/source/libraries/${library}/files/${sourceFile}/members`)

export const fetchMember = (library: string, sourceFile: string, member: string) =>
  request.get<{ content?: string }>(`/source/libraries/${library}/files/${sourceFile}/members/${member}`)
