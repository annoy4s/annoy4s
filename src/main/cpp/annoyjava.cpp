// Copyright (c) 2016 pishen
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

#include "annoylib.h"
#include "kissrandom.h"

extern "C" {
AnnoyIndexInterface<int32_t, float> *anCreateAngular(int f) {
  return new AnnoyIndex<int32_t, float, Angular, Kiss64Random>(f);
}

AnnoyIndexInterface<int32_t, float> *anCreateEuclidean(int f) {
  return new AnnoyIndex<int32_t, float, Euclidean, Kiss64Random>(f);
}

void anDeleteIndex(AnnoyIndexInterface<int32_t, float> *ptr) {
  delete ptr;
}

void anAddItem(AnnoyIndexInterface<int32_t, float> *ptr, int item, float *w) {
  ptr->add_item(item, w);
}

void anBuild(AnnoyIndexInterface<int32_t, float> *ptr, int q) {
  ptr->build(q);
}

bool anSave(AnnoyIndexInterface<int32_t, float> *ptr, char *filename) {
  return ptr->save(filename);
}

void anUnload(AnnoyIndexInterface<int32_t, float> *ptr) {
  ptr->unload();
}

bool anLoad(AnnoyIndexInterface<int32_t, float> *ptr, char *filename) {
  return ptr->load(filename);
}

float anGetDistance(AnnoyIndexInterface<int32_t, float> *ptr, int i, int j) {
  return ptr->get_distance(i, j);
}

void anGetNnsByItem(AnnoyIndexInterface<int32_t, float> *ptr, int item, int n,
                    int search_k, int *result, float *distances) {
  vector<int32_t> resultV;
  vector<float> distancesV;
  ptr->get_nns_by_item(item, n, search_k, &resultV, &distancesV);
  std::copy(resultV.begin(), resultV.end(), result);
  std::copy(distancesV.begin(), distancesV.end(), distances);
}

int anGetNItems(AnnoyIndexInterface<int32_t, float> *ptr) {
  return (int)ptr->get_n_items();
}

void anVerbose(AnnoyIndexInterface<int32_t, float> *ptr, bool v) {
  ptr->verbose(v);
}

void anGetItem(AnnoyIndexInterface<int32_t, float> *ptr, int item, float *v) {
  ptr->get_item(item, v);
}
}
