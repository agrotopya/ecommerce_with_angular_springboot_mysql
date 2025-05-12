// src/app/shared/models/address.model.ts

export interface AddressDto { // Address -> AddressDto
  fullName: string; // Eklendi
  street: string; // addressLine1 -> street olarak geri değiştirildi (backend validasyonuna göre)
  addressLine2?: string; // Eklendi
  city: string;
  state: string; // Eyalet veya ilçe
  zipCode: string;
  country: string;
  phoneNumber: string; // Opsiyonel değil, zorunlu yapıldı (apidocs.md'ye göre)
  addressTitle?: string; // Opsiyonel: Ev, İş vb. (apidocs.md'de yok, ama kalabilir)
}
