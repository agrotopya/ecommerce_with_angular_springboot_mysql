import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FeelCardComponent } from './feel-card.component';

describe('FeelCardComponent', () => {
  let component: FeelCardComponent;
  let fixture: ComponentFixture<FeelCardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FeelCardComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(FeelCardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
