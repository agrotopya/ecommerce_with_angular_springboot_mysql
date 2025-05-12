export interface CategoryRequestDto {
  name: string;
  slug?: string;
  description?: string;
  parentId?: number | null;
  isActive?: boolean;
  displayOrder?: number;
  imageUrl?: string;
}

export interface CategoryResponseDto {
  id: number;
  name: string;
  slug: string;
  description?: string;
  parentId?: number | null;
  parentName?: string | null; // Might be useful to display
  isActive: boolean;
  displayOrder?: number;
  imageUrl?: string;
  createdAt: string; // Or Date
  updatedAt: string; // Or Date
  childrenCount?: number; // If API provides this for tree view or list
  // children?: CategoryResponseDto[]; // For tree structure if needed directly in DTO
}

// For hierarchical data, if the API returns it in a specific tree format
export interface CategoryTreeNodeDto extends CategoryResponseDto {
  children?: CategoryTreeNodeDto[];
  expanded?: boolean; // For UI state
}
